package br.com.dadoscnpj.payment;

import br.com.dadoscnpj.config.MercadoPagoProperties;
import br.com.dadoscnpj.domain.SubscriptionPlan;
import br.com.dadoscnpj.dto.ChargePlanRequest;
import br.com.dadoscnpj.dto.ChargePlanResponse;
import br.com.dadoscnpj.dto.CheckoutResponse;
import br.com.dadoscnpj.dto.PaymentConfigResponse;
import br.com.dadoscnpj.dto.PaymentHistoryItemResponse;
import br.com.dadoscnpj.dto.SavedCardResponse;
import br.com.dadoscnpj.entity.PaymentOrderEntity;
import br.com.dadoscnpj.entity.UserEntity;
import br.com.dadoscnpj.plan.PlanLimitsService;
import br.com.dadoscnpj.repository.PaymentOrderRepository;
import br.com.dadoscnpj.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MercadoPagoPaymentService(MercadoPagoProperties properties,
                                     PaymentOrderRepository paymentOrderRepository,
                                     UserRepository userRepository,
                                     PlanLimitsService planLimitsService,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.paymentOrderRepository = paymentOrderRepository;
        this.userRepository = userRepository;
        this.planLimitsService = planLimitsService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .build();
    }

    @Transactional
    public CheckoutResponse criarCheckout(UUID userId, SubscriptionPlan plan) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Pagamentos não configurados. Defina MERCADOPAGO_ACCESS_TOKEN no servidor.");
        }
        if (plan != SubscriptionPlan.PREMIUM && plan != SubscriptionPlan.PRO_PLUS) {
            throw new IllegalArgumentException("Plano inválido para checkout");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        UUID orderId = UUID.randomUUID();
        int amountCents = plan == SubscriptionPlan.PREMIUM
                ? properties.getPremiumPriceCents()
                : properties.getProPlusPriceCents();

        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(orderId);
        order.setUserId(userId);
        order.setPlan(plan);
        order.setStatus("PENDING");
        order.setAmountCents(amountCents);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        paymentOrderRepository.save(order);

        Map<String, Object> body = montarPreferencia(user, orderId, plan, amountCents);
        JsonNode response = restClient.post()
                .uri("/checkout/preferences")
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

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
        return checkout;
    }

    public PaymentConfigResponse obterConfig() {
        return new PaymentConfigResponse(
                properties.getPublicKey(),
                properties.isCheckoutReady());
    }

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
            return cards;
        } catch (RestClientResponseException e) {
            log.warn("Falha ao listar cartões MP para usuário {}: {}", userId, e.getMessage());
            return List.of();
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

        UserEntity user = obterOuCriarCustomer(userId);
        Map<String, Object> body = Map.of("token", token);

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
        return mapearCartao(response);
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

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String paymentToken = resolverTokenPagamento(user, request);
        int amountCents = plan == SubscriptionPlan.PREMIUM
                ? properties.getPremiumPriceCents()
                : properties.getProPlusPriceCents();

        UUID orderId = UUID.randomUUID();
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(orderId);
        order.setUserId(userId);
        order.setPlan(plan);
        order.setStatus("PENDING");
        order.setAmountCents(amountCents);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        paymentOrderRepository.save(order);

        Map<String, Object> payer = new HashMap<>();
        payer.put("email", user.getEmail());
        payer.put("identification", Map.of("type", "CPF", "number", user.getCpf()));

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", amountCents / 100.0);
        body.put("token", paymentToken);
        body.put("description", plan == SubscriptionPlan.PREMIUM ? "LupaCNPJ Prospecção" : "LupaCNPJ Growth");
        body.put("installments", 1);
        body.put("external_reference", orderId.toString());
        body.put("payer", payer);
        body.put("notification_url", properties.getApiPublicUrl().replaceAll("/$", "") + "/payments/mercadopago/webhook");

        JsonNode payment = restClient.post()
                .uri("/v1/payments")
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .header("X-Idempotency-Key", orderId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (payment == null) {
            throw new IllegalStateException("Falha ao processar pagamento");
        }

        String status = payment.path("status").asText("pending").toUpperCase(Locale.ROOT);
        String paymentId = payment.path("id").asText(null);

        order.setMpPaymentId(paymentId);
        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        paymentOrderRepository.save(order);

        if ("APPROVED".equalsIgnoreCase(status)) {
            user.setPlan(plan);
            userRepository.save(user);
        }

        ChargePlanResponse response = new ChargePlanResponse();
        response.setOrderId(orderId.toString());
        response.setStatus(status);
        response.setStatusLabel(rotuloStatus(status));
        response.setPlanNome(planLimitsService.nomeExibicao(plan));
        return response;
    }

    private String resolverTokenPagamento(UserEntity user, ChargePlanRequest request) {
        if (request.getToken() != null && !request.getToken().isBlank()) {
            return request.getToken();
        }
        if (request.getCardId() == null || request.getCardId().isBlank()) {
            throw new IllegalArgumentException("Informe o cartão ou cadastre um novo");
        }
        if (request.getSecurityCode() == null || request.getSecurityCode().isBlank()) {
            throw new IllegalArgumentException("Informe o CVV do cartão");
        }
        if (user.getMpCustomerId() == null || user.getMpCustomerId().isBlank()) {
            throw new IllegalArgumentException("Cadastre um cartão antes de assinar");
        }

        Map<String, Object> body = Map.of(
                "card_id", request.getCardId(),
                "security_code", request.getSecurityCode());

        JsonNode tokenResponse = restClient.post()
                .uri("/v1/card_tokens")
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (tokenResponse == null || !tokenResponse.has("id")) {
            throw new IllegalStateException("Não foi possível validar o cartão. Verifique o CVV.");
        }
        return tokenResponse.get("id").asText();
    }

    private UserEntity obterOuCriarCustomer(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (user.getMpCustomerId() != null && !user.getMpCustomerId().isBlank()) {
            return user;
        }

        Map<String, Object> body = Map.of("email", user.getEmail());
        JsonNode response = restClient.post()
                .uri("/v1/customers")
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("id")) {
            throw new IllegalStateException("Falha ao criar cliente no Mercado Pago");
        }

        user.setMpCustomerId(response.get("id").asText());
        return userRepository.save(user);
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
        paymentOrderRepository.save(order);

        if ("APPROVED".equalsIgnoreCase(status)) {
            UserEntity user = userRepository.findById(order.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Usuário do pagamento não encontrado"));
            user.setPlan(order.getPlan());
            userRepository.save(user);
            log.info("Plano {} ativado para usuário {} via Mercado Pago", order.getPlan(), user.getEmail());
        }
    }

    private Map<String, Object> montarPreferencia(UserEntity user, UUID orderId, SubscriptionPlan plan, int amountCents) {
        String titulo = plan == SubscriptionPlan.PREMIUM ? "LupaCNPJ Premium" : "LupaCNPJ Pro+";
        String frontend = properties.getFrontendUrl().replaceAll("/$", "");

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
        body.put("auto_return", "approved");
        body.put("external_reference", orderId.toString());
        body.put("notification_url", properties.getApiPublicUrl().replaceAll("/$", "") + "/payments/mercadopago/webhook");
        body.put("payment_methods", Map.of(
                "excluded_payment_types", java.util.List.of(),
                "installments", 1
        ));
        return body;
    }
}
