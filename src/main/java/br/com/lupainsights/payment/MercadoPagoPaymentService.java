package br.com.lupainsights.payment;

import br.com.lupainsights.config.MercadoPagoProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.dto.ChargePlanRequest;
import br.com.lupainsights.dto.ChargePlanResponse;
import br.com.lupainsights.dto.CheckoutRequest;
import br.com.lupainsights.dto.CheckoutSyncRequest;
import br.com.lupainsights.dto.CheckoutSyncResponse;
import br.com.lupainsights.dto.CheckoutResponse;
import br.com.lupainsights.dto.PaymentConfigResponse;
import br.com.lupainsights.dto.PaymentHistoryItemResponse;
import br.com.lupainsights.dto.PlanQuoteResponse;
import br.com.lupainsights.dto.SubscriptionStatusResponse;
import br.com.lupainsights.dto.SavedCardResponse;
import br.com.lupainsights.entity.PaymentOrderEntity;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.exception.ForbiddenException;
import br.com.lupainsights.plan.PlanLimitsService;
import br.com.lupainsights.repository.PaymentOrderRepository;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.subscription.SubscriptionService;
import br.com.lupainsights.service.TrialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class MercadoPagoPaymentService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentService.class);

    private final MercadoPagoProperties properties;
    private final PaymentOrderRepository paymentOrderRepository;
    private final UserRepository userRepository;
    private final PlanLimitsService planLimitsService;
    private final MercadoPagoCustomerService customerService;
    private final SubscriptionService subscriptionService;
    private final TrialService trialService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MercadoPagoPaymentService(MercadoPagoProperties properties,
                                     PaymentOrderRepository paymentOrderRepository,
                                     UserRepository userRepository,
                                     PlanLimitsService planLimitsService,
                                     MercadoPagoCustomerService customerService,
                                     SubscriptionService subscriptionService,
                                     @Lazy TrialService trialService,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.paymentOrderRepository = paymentOrderRepository;
        this.userRepository = userRepository;
        this.planLimitsService = planLimitsService;
        this.customerService = customerService;
        this.subscriptionService = subscriptionService;
        this.trialService = trialService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .build();
    }

    @Transactional
    public CheckoutResponse criarCheckout(UUID userId, CheckoutRequest request) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Pagamentos não configurados. Defina MERCADOPAGO_ACCESS_TOKEN no servidor.");
        }
        SubscriptionPlan plan = request.getPlan();
        if (plan != SubscriptionPlan.PREMIUM && plan != SubscriptionPlan.PRO_PLUS) {
            throw new IllegalArgumentException("Plano inválido para checkout");
        }
        validarParcelas(request.getInstallments());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        UUID orderId = UUID.randomUUID();
        int amountCents = subscriptionService.calcularValorCobranca(user, plan);
        boolean upgrade = subscriptionService.ehUpgradeProporcional(user, plan);

        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(orderId);
        order.setUserId(userId);
        order.setPlan(plan);
        order.setStatus("PENDING");
        order.setAmountCents(amountCents);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        paymentOrderRepository.save(order);

        Map<String, Object> body = montarPreferencia(user, orderId, plan, amountCents, upgrade);
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw customerService.traduzirErro("Não foi possível iniciar o checkout", e);
        }

        if (response == null || !response.has("id")) {
            throw new IllegalStateException("Falha ao criar preferência no Mercado Pago");
        }

        String preferenceId = response.get("id").asText();
        order.setMpPreferenceId(preferenceId);
        order.setUpdatedAt(Instant.now());
        paymentOrderRepository.save(order);

        CheckoutResponse checkout = new CheckoutResponse();
        checkout.setOrderId(orderId.toString());
        checkout.setPreferenceId(preferenceId);
        checkout.setInitPoint(response.path("init_point").asText(null));
        checkout.setSandboxInitPoint(response.path("sandbox_init_point").asText(null));
        checkout.setAmountCents(amountCents);
        checkout.setAmountLabel(formatarValor(amountCents));
        checkout.setUpgrade(upgrade);
        return checkout;
    }

    public PlanQuoteResponse obterCotacao(UUID userId, SubscriptionPlan plan, Integer installments) {
        if (plan != SubscriptionPlan.PREMIUM && plan != SubscriptionPlan.PRO_PLUS) {
            throw new IllegalArgumentException("Plano inválido para cotação");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        subscriptionService.expirarSeNecessario(user);
        return subscriptionService.montarCotacao(user, plan, installments);
    }

    public PaymentConfigResponse obterConfig() {
        return new PaymentConfigResponse(
                properties.getPublicKey(),
                properties.isCheckoutReady());
    }

    @Transactional
    public List<SavedCardResponse> listarCartoes(UUID userId) {
        if (!properties.isConfigured()) {
            return List.of();
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (user.getMpCustomerId() == null || user.getMpCustomerId().isBlank()) {
            return List.of();
        }

        try {
            JsonNode response = restClient.get()
                    .uri("/v1/customers/{id}/cards", user.getMpCustomerId())
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray()) {
                return List.of();
            }
            List<SavedCardResponse> cards = new ArrayList<>();
            for (JsonNode node : response) {
                cards.add(mapearCartao(node));
            }
            sincronizarCartaoPadrao(user, cards);
            for (SavedCardResponse card : cards) {
                card.setDefaultCard(card.getId().equals(user.getDefaultCardId()));
            }
            return cards;
        } catch (RestClientResponseException e) {
            log.warn("Falha ao listar cartões MP para usuário {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private void sincronizarCartaoPadrao(UserEntity user, List<SavedCardResponse> cards) {
        if (cards.isEmpty()) {
            if (user.getDefaultCardId() != null && !user.getDefaultCardId().isBlank()) {
                user.setDefaultCardId(null);
                userRepository.save(user);
            }
            return;
        }
        boolean padraoValido = user.getDefaultCardId() != null
                && cards.stream().anyMatch(c -> c.getId().equals(user.getDefaultCardId()));
        if (!padraoValido) {
            subscriptionService.definirCartaoPadrao(user, cards.get(0).getId());
        }
    }

    @Transactional
    public SavedCardResponse salvarCartao(UUID userId, String token) {
        if (!properties.isCheckoutReady()) {
            throw new IllegalStateException("Pagamentos não configurados. Defina MERCADOPAGO_ACCESS_TOKEN e MERCADOPAGO_PUBLIC_KEY.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token do cartão é obrigatório");
        }

        UserEntity user = customerService.obterOuCriar(userId);
        Map<String, Object> body = Map.of("token", token);

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/customers/{id}/cards", user.getMpCustomerId())
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("id")) {
                throw new IllegalStateException("Não foi possível salvar o cartão. Verifique os dados e tente novamente.");
            }
            SavedCardResponse card = mapearCartao(response);
            subscriptionService.definirCartaoPadrao(user, card.getId());
            trialService.habilitarConversaoAoSalvarCartao(user);
            card.setDefaultCard(true);
            return card;
        } catch (RestClientResponseException e) {
            throw customerService.traduzirErro("Não foi possível salvar o cartão", e);
        }
    }

    @Transactional
    public void removerCartao(UUID userId, String cardId) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Pagamentos não configurados.");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (user.getMpCustomerId() == null || user.getMpCustomerId().isBlank()) {
            throw new IllegalArgumentException("Nenhum cartão cadastrado");
        }

        restClient.delete()
                .uri("/v1/customers/{customerId}/cards/{cardId}", user.getMpCustomerId(), cardId)
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .retrieve()
                .toBodilessEntity();

        if (cardId.equals(user.getDefaultCardId())) {
            user.setDefaultCardId(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public ChargePlanResponse cobrarPlano(UUID userId, ChargePlanRequest request) {
        if (!properties.isCheckoutReady()) {
            throw new IllegalStateException("Pagamentos não configurados.");
        }
        SubscriptionPlan plan = request.getPlan();
        if (plan != SubscriptionPlan.PREMIUM && plan != SubscriptionPlan.PRO_PLUS) {
            throw new IllegalArgumentException("Plano inválido para cobrança");
        }

        boolean cartaoSalvo = usaCartaoSalvo(request);
        UserEntity user = cartaoSalvo
                ? customerService.obterOuCriar(userId)
                : userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        JsonNode cartaoMp = null;
        if (cartaoSalvo && request.getCardId() != null && !request.getCardId().isBlank()) {
            cartaoMp = buscarCartaoSalvo(user, request.getCardId());
        }

        String paymentToken = resolverTokenPagamento(user, request);
        int amountCents = subscriptionService.calcularValorCobranca(user, plan);
        int installments = validarParcelas(request.getInstallments());

        UUID orderId = UUID.randomUUID();
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(orderId);
        order.setUserId(userId);
        order.setPlan(plan);
        order.setStatus("PENDING");
        order.setAmountCents(amountCents);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setRenewal(request.isRenewal());
        paymentOrderRepository.save(order);

        Map<String, Object> payer = montarPayer(user, cartaoSalvo);

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", amountCents / 100.0);
        body.put("token", paymentToken);
        body.put("description", descricaoPagamento(plan, subscriptionService.ehUpgradeProporcional(user, plan)));
        body.put("installments", installments);
        body.put("external_reference", orderId.toString());
        body.put("payer", payer);
        if (cartaoMp != null) {
            adicionarDadosCartaoSalvo(body, cartaoMp);
        }
        adicionarNotificationUrlSeAplicavel(body);

        JsonNode payment;
        try {
            payment = restClient.post()
                    .uri("/v1/payments")
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .header("X-Idempotency-Key", orderId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw customerService.traduzirErro("Não foi possível processar o pagamento", e);
        }

        if (payment == null) {
            throw new IllegalStateException("Falha ao processar pagamento");
        }

        String status = payment.path("status").asText("pending").toUpperCase(Locale.ROOT);
        String paymentId = payment.path("id").asText(null);

        order.setMpPaymentId(paymentId);
        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        if ("APPROVED".equalsIgnoreCase(status)) {
            order.setPaidAt(Instant.now());
        }
        paymentOrderRepository.save(order);

        if ("APPROVED".equalsIgnoreCase(status)) {
            aplicarPagamentoAprovado(user, plan, order, request.getCardId());
        }

        ChargePlanResponse response = new ChargePlanResponse();
        response.setOrderId(orderId.toString());
        response.setStatus(status);
        response.setStatusLabel(rotuloStatus(status));
        response.setPlanNome(planLimitsService.nomeExibicao(plan));
        return response;
    }

    @Transactional
    public void cobrarRenovacaoAutomatica(UserEntity user) {
        if (user.getDefaultCardId() == null || user.getDefaultCardId().isBlank()) {
            log.warn("Renovação ignorada para {}: sem cartão padrão", user.getEmail());
            return;
        }
        ChargePlanRequest request = new ChargePlanRequest();
        request.setPlan(user.getPlan());
        request.setCardId(user.getDefaultCardId());
        request.setRenewal(true);
        try {
            cobrarPlano(user.getId(), request);
        } catch (Exception e) {
            log.warn("Renovação automática falhou para {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public SubscriptionStatusResponse obterStatusAssinatura(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        subscriptionService.expirarSeNecessario(user);
        return subscriptionService.montarStatus(user);
    }

    @Transactional
    public UserEntity cancelarAssinatura(UUID userId) {
        return subscriptionService.cancelar(userId);
    }

    @Transactional
    public UserEntity reativarAssinatura(UUID userId) {
        return subscriptionService.reativar(userId);
    }

    private void aplicarPagamentoAprovado(UserEntity user, SubscriptionPlan plan,
                                          PaymentOrderEntity order, String cardId) {
        Instant paidAt = order.getPaidAt() != null ? order.getPaidAt() : Instant.now();
        if (order.isRenewal()) {
            subscriptionService.ativarPeriodoPago(user, plan, paidAt, true);
        } else if (subscriptionService.ehUpgradeProporcional(user, plan)) {
            subscriptionService.aplicarUpgrade(user, plan);
        } else {
            subscriptionService.ativarPeriodoPago(user, plan, paidAt, false);
        }
        if (cardId != null && !cardId.isBlank()) {
            subscriptionService.definirCartaoPadrao(user, cardId);
        }
        log.info("Assinatura {} ativada/renovada para {} até {}",
                plan, user.getEmail(), user.getPlanValidUntil());
    }

    private boolean usaCartaoSalvo(ChargePlanRequest request) {
        if (request.getCardId() != null && !request.getCardId().isBlank()) {
            return true;
        }
        return request.getToken() != null && !request.getToken().isBlank();
    }

    private Map<String, Object> montarPayer(UserEntity user, boolean cartaoSalvo) {
        Map<String, Object> payer = new HashMap<>();
        payer.put("email", user.getEmail());
        if (cartaoSalvo && user.getMpCustomerId() != null && !user.getMpCustomerId().isBlank()) {
            payer.put("type", "customer");
            payer.put("id", user.getMpCustomerId());
        } else {
            payer.put("identification", Map.of("type", "CPF", "number", user.getCpf()));
        }
        return payer;
    }

    private String resolverTokenPagamento(UserEntity user, ChargePlanRequest request) {
        if (request.getToken() != null && !request.getToken().isBlank()) {
            return request.getToken();
        }
        if (request.getCardId() == null || request.getCardId().isBlank()) {
            throw new IllegalArgumentException("Informe o cartão ou cadastre um novo");
        }
        if (!request.isRenewal()
                && (request.getSecurityCode() == null || request.getSecurityCode().isBlank())) {
            throw new IllegalArgumentException("Informe o CVV do cartão");
        }
        if (user.getMpCustomerId() == null || user.getMpCustomerId().isBlank()) {
            throw new IllegalArgumentException("Cadastre um cartão antes de assinar");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("card_id", request.getCardId());
        body.put("customer_id", user.getMpCustomerId());
        if (request.getSecurityCode() != null && !request.getSecurityCode().isBlank()) {
            body.put("security_code", request.getSecurityCode());
        }

        JsonNode tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/card_tokens")
                            .queryParam("public_key", properties.getPublicKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw customerService.traduzirErro("Não foi possível validar o cartão", e);
        }

        if (tokenResponse == null || !tokenResponse.has("id")) {
            throw new IllegalStateException("Não foi possível validar o cartão. Verifique o CVV.");
        }
        return tokenResponse.get("id").asText();
    }

    private JsonNode buscarCartaoSalvo(UserEntity user, String cardId) {
        if (user.getMpCustomerId() == null || user.getMpCustomerId().isBlank()) {
            throw new IllegalArgumentException("Cadastre um cartão antes de assinar");
        }
        try {
            JsonNode card = restClient.get()
                    .uri("/v1/customers/{customerId}/cards/{cardId}",
                            user.getMpCustomerId(), cardId)
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
            if (card == null || !card.has("id")) {
                throw new IllegalArgumentException(
                        "Cartão salvo não encontrado no Mercado Pago. Cadastre novamente em Cobrança.");
            }
            return card;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException(
                        "Cartão salvo não encontrado no Mercado Pago. Cadastre novamente em Cobrança.");
            }
            throw customerService.traduzirErro("Não foi possível validar o cartão salvo", e);
        }
    }

    private void adicionarDadosCartaoSalvo(Map<String, Object> body, JsonNode cartaoMp) {
        String paymentMethodId = cartaoMp.path("payment_method").path("id").asText(null);
        if (paymentMethodId != null && !paymentMethodId.isBlank()) {
            body.put("payment_method_id", paymentMethodId);
        }
    }

    private SavedCardResponse mapearCartao(JsonNode node) {
        SavedCardResponse card = new SavedCardResponse();
        card.setId(node.path("id").asText(null));
        card.setBrand(node.path("payment_method").path("name").asText(
                node.path("issuer").path("name").asText("Cartão")));
        card.setLastFourDigits(node.path("last_four_digits").asText(
                node.path("last4").asText("****")));
        card.setExpirationMonth(node.path("expiration_month").asText(""));
        card.setExpirationYear(node.path("expiration_year").asText(""));
        card.setHolderName(node.path("cardholder").path("name").asText(""));
        return card;
    }

    public List<PaymentHistoryItemResponse> listarHistorico(UUID userId) {
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::paraHistorico)
                .toList();
    }

    private PaymentHistoryItemResponse paraHistorico(PaymentOrderEntity order) {
        PaymentHistoryItemResponse item = new PaymentHistoryItemResponse();
        item.setId(order.getId().toString());
        item.setPlanNome(planLimitsService.nomeExibicao(order.getPlan()));
        item.setAmountLabel(formatarValor(order.getAmountCents()));
        item.setStatus(order.getStatus());
        item.setStatusLabel(rotuloStatus(order.getStatus()));
        item.setCreatedAt(order.getCreatedAt());
        return item;
    }

    private String formatarValor(int amountCents) {
        double valor = amountCents / 100.0;
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valor);
    }

    private String rotuloStatus(String status) {
        if (status == null) {
            return "Desconhecido";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "APPROVED" -> "Aprovado";
            case "PENDING" -> "Pendente";
            case "REJECTED", "CANCELLED" -> "Recusado";
            default -> status;
        };
    }

    @Transactional
    public CheckoutSyncResponse sincronizarCheckoutRetorno(UUID userId, CheckoutSyncRequest request) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Pagamentos não configurados.");
        }

        PaymentOrderEntity order = resolverOrdemCheckout(userId, request);
        if (order == null) {
            throw new IllegalArgumentException("Pedido de pagamento não encontrado.");
        }

        if ("APPROVED".equalsIgnoreCase(order.getStatus())) {
            return montarSyncResponse(order);
        }

        String paymentId = request.getPaymentId();
        if (paymentId == null || paymentId.isBlank()) {
            paymentId = buscarPaymentIdMercadoPago(order.getId());
        }
        if (paymentId == null || paymentId.isBlank()) {
            CheckoutSyncResponse pendente = montarSyncResponse(order);
            pendente.setStatus("PENDING");
            pendente.setStatusLabel(rotuloStatus("PENDING"));
            return pendente;
        }

        validarPagamentoDoUsuario(userId, paymentId);
        processarNotificacao(paymentId);

        PaymentOrderEntity atualizado = paymentOrderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado após sincronização"));
        return montarSyncResponse(atualizado);
    }

    private PaymentOrderEntity resolverOrdemCheckout(UUID userId, CheckoutSyncRequest request) {
        if (request.getOrderId() != null && !request.getOrderId().isBlank()) {
            return carregarOrdemDoUsuario(userId, request.getOrderId());
        }
        if (request.getExternalReference() != null && !request.getExternalReference().isBlank()) {
            return carregarOrdemDoUsuario(userId, request.getExternalReference());
        }
        if (request.getPreferenceId() != null && !request.getPreferenceId().isBlank()) {
            PaymentOrderEntity porPreferencia = paymentOrderRepository
                    .findByMpPreferenceId(request.getPreferenceId())
                    .orElse(null);
            if (porPreferencia != null && porPreferencia.getUserId().equals(userId)) {
                return porPreferencia;
            }
            return null;
        }
        if (request.getPaymentId() != null && !request.getPaymentId().isBlank()) {
            JsonNode payment = buscarPagamentoMercadoPago(request.getPaymentId());
            if (payment != null) {
                String externalReference = payment.path("external_reference").asText("");
                if (!externalReference.isBlank()) {
                    return carregarOrdemDoUsuario(userId, externalReference);
                }
            }
        }

        List<PaymentOrderEntity> pendentes =
                paymentOrderRepository.findTop3ByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING");
        return pendentes.isEmpty() ? null : pendentes.get(0);
    }

    private PaymentOrderEntity carregarOrdemDoUsuario(UUID userId, String orderIdText) {
        UUID orderId;
        try {
            orderId = UUID.fromString(orderIdText);
        } catch (IllegalArgumentException e) {
            return null;
        }
        PaymentOrderEntity order = paymentOrderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return order;
    }

    private void validarPagamentoDoUsuario(UUID userId, String paymentId) {
        JsonNode payment = buscarPagamentoMercadoPago(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Pagamento não encontrado no Mercado Pago.");
        }
        String externalReference = payment.path("external_reference").asText("");
        PaymentOrderEntity order = carregarOrdemDoUsuario(userId, externalReference);
        if (order == null) {
            throw new ForbiddenException("Pagamento não pertence a este usuário.");
        }
    }

    private JsonNode buscarPagamentoMercadoPago(String paymentId) {
        try {
            return restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            log.warn("Falha ao buscar pagamento {} no MP: {}", paymentId, e.getMessage());
            return null;
        }
    }

    private String buscarPaymentIdMercadoPago(UUID orderId) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/payments/search")
                            .queryParam("external_reference", orderId.toString())
                            .queryParam("sort", "date_created")
                            .queryParam("criteria", "desc")
                            .build())
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("results") || !response.get("results").isArray()) {
                return null;
            }
            JsonNode results = response.get("results");
            if (results.isEmpty()) {
                return null;
            }
            return results.get(0).path("id").asText(null);
        } catch (RestClientResponseException e) {
            log.warn("Falha ao buscar pagamentos por external_reference {}: {}", orderId, e.getMessage());
            return null;
        }
    }

    private CheckoutSyncResponse montarSyncResponse(PaymentOrderEntity order) {
        CheckoutSyncResponse response = new CheckoutSyncResponse();
        response.setOrderId(order.getId().toString());
        response.setStatus(order.getStatus());
        response.setStatusLabel(rotuloStatus(order.getStatus()));
        response.setPlanNome(planLimitsService.nomeExibicao(order.getPlan()));
        return response;
    }

    @Transactional
    public void processarNotificacao(String paymentId) {
        if (!properties.isConfigured() || paymentId == null || paymentId.isBlank()) {
            return;
        }

        JsonNode payment = restClient.get()
                .uri("/v1/payments/{id}", paymentId)
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .retrieve()
                .body(JsonNode.class);

        if (payment == null) {
            return;
        }

        String status = payment.path("status").asText("");
        String externalReference = payment.path("external_reference").asText("");
        if (externalReference.isBlank()) {
            return;
        }

        UUID orderId;
        try {
            orderId = UUID.fromString(externalReference);
        } catch (IllegalArgumentException e) {
            log.warn("external_reference inválido no pagamento {}: {}", paymentId, externalReference);
            return;
        }

        PaymentOrderEntity order = paymentOrderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }

        order.setMpPaymentId(paymentId);
        order.setStatus(status.toUpperCase());
        order.setUpdatedAt(Instant.now());
        if ("APPROVED".equalsIgnoreCase(status)) {
            order.setPaidAt(Instant.now());
        }
        paymentOrderRepository.save(order);

        if ("APPROVED".equalsIgnoreCase(status)) {
            UserEntity user = userRepository.findById(order.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Usuário do pagamento não encontrado"));
            aplicarPagamentoAprovado(user, order.getPlan(), order, null);
        }
    }

    private void adicionarNotificationUrlSeAplicavel(Map<String, Object> body) {
        String base = properties.getApiPublicUrl();
        if (base == null || base.isBlank()) {
            return;
        }
        String webhookUrl = base.replaceAll("/$", "") + "/payments/mercadopago/webhook";
        if (webhookUrl.contains("localhost") || webhookUrl.contains("127.0.0.1")) {
            return;
        }
        body.put("notification_url", webhookUrl);
    }

    private Map<String, Object> montarPreferencia(UserEntity user, UUID orderId, SubscriptionPlan plan,
                                                  int amountCents, boolean upgrade) {
        String titulo = descricaoPagamento(plan, upgrade);
        String frontend = properties.getFrontendUrl().replaceAll("/$", "");
        boolean frontendLocal = frontend.contains("localhost") || frontend.contains("127.0.0.1");

        Map<String, Object> item = Map.of(
                "title", titulo,
                "quantity", 1,
                "currency_id", "BRL",
                "unit_price", amountCents / 100.0
        );

        Map<String, Object> backUrls = new HashMap<>();
        backUrls.put("success", frontend + "/planos/sucesso");
        backUrls.put("failure", frontend + "/planos/pendente");
        backUrls.put("pending", frontend + "/planos/pendente");

        Map<String, Object> body = new HashMap<>();
        body.put("items", java.util.List.of(item));
        body.put("payer", Map.of("email", user.getEmail()));
        body.put("back_urls", backUrls);
        if (!frontendLocal) {
            body.put("auto_return", "approved");
        }
        body.put("external_reference", orderId.toString());
        adicionarNotificationUrlSeAplicavel(body);
        body.put("payment_methods", Map.of(
                "excluded_payment_types", java.util.List.of(
                        Map.of("id", "ticket"),
                        Map.of("id", "atm"),
                        Map.of("id", "bank_transfer")
                ),
                "installments", 12
        ));
        return body;
    }

    private int validarParcelas(Integer installments) {
        int parcelas = installments == null ? 1 : installments;
        if (parcelas < 1 || parcelas > 12) {
            throw new IllegalArgumentException("Parcelas devem ser entre 1 e 12");
        }
        return parcelas;
    }

    private String descricaoPagamento(SubscriptionPlan plan, boolean upgrade) {
        if (upgrade) {
            return "Upgrade Lupa Insights Growth (proporcional)";
        }
        return plan == SubscriptionPlan.PREMIUM ? "Lupa Insights Prospecção" : "Lupa Insights Growth";
    }
}
