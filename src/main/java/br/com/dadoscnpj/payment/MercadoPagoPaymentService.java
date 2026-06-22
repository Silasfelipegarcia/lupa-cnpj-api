package br.com.dadoscnpj.payment;

import br.com.dadoscnpj.config.MercadoPagoProperties;
import br.com.dadoscnpj.domain.SubscriptionPlan;
import br.com.dadoscnpj.dto.CheckoutResponse;
import br.com.dadoscnpj.dto.PaymentHistoryItemResponse;
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

import java.time.Instant;
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
