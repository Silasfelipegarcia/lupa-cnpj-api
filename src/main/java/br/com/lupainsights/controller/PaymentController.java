package br.com.lupainsights.controller;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.dto.ChargePlanRequest;
import br.com.lupainsights.dto.ChargePlanResponse;
import br.com.lupainsights.dto.CheckoutRequest;
import br.com.lupainsights.dto.CheckoutResponse;
import br.com.lupainsights.dto.PaymentConfigResponse;
import br.com.lupainsights.dto.PaymentHistoryItemResponse;
import br.com.lupainsights.dto.PlanQuoteResponse;
import br.com.lupainsights.dto.SubscriptionStatusResponse;
import br.com.lupainsights.dto.SaveCardRequest;
import br.com.lupainsights.dto.SavedCardResponse;
import br.com.lupainsights.payment.IdempotencyService;
import br.com.lupainsights.payment.MercadoPagoPaymentService;
import br.com.lupainsights.payment.MercadoPagoWebhookVerifier;
import br.com.lupainsights.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final String CHECKOUT_ENDPOINT = "/payments/checkout";
    private static final String CHARGE_ENDPOINT = "/payments/charge";

    private final MercadoPagoPaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final MercadoPagoWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    public PaymentController(MercadoPagoPaymentService paymentService,
                             IdempotencyService idempotencyService,
                             MercadoPagoWebhookVerifier webhookVerifier,
                             ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CheckoutRequest request) throws Exception {
        UUID userId = SecurityUtils.currentUserId();
        Optional<IdempotencyService.StoredResponse> cached =
                idempotencyService.buscar(userId, CHECKOUT_ENDPOINT, idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.status(cached.get().statusCode())
                    .body(objectMapper.readValue(cached.get().responseBody(), CheckoutResponse.class));
        }

        CheckoutResponse checkout = paymentService.criarCheckout(userId, request.getPlan());
        ResponseEntity<CheckoutResponse> response = ResponseEntity.ok(checkout);
        idempotencyService.salvar(userId, CHECKOUT_ENDPOINT, idempotencyKey, response);
        return response;
    }

    @GetMapping("/quote")
    public ResponseEntity<PlanQuoteResponse> cotacao(@RequestParam SubscriptionPlan plan) {
        return ResponseEntity.ok(paymentService.obterCotacao(SecurityUtils.currentUserId(), plan));
    }

    @GetMapping("/config")
    public ResponseEntity<PaymentConfigResponse> config() {
        return ResponseEntity.ok(paymentService.obterConfig());
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryItemResponse>> historico() {
        return ResponseEntity.ok(paymentService.listarHistorico(SecurityUtils.currentUserId()));
    }

    @GetMapping("/cards")
    public ResponseEntity<List<SavedCardResponse>> cartoes() {
        return ResponseEntity.ok(paymentService.listarCartoes(SecurityUtils.currentUserId()));
    }

    @PostMapping("/cards")
    public ResponseEntity<SavedCardResponse> salvarCartao(@RequestBody SaveCardRequest request) {
        return ResponseEntity.status(201).body(
                paymentService.salvarCartao(SecurityUtils.currentUserId(), request.getToken()));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> removerCartao(@org.springframework.web.bind.annotation.PathVariable String cardId) {
        paymentService.removerCartao(SecurityUtils.currentUserId(), cardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/charge")
    public ResponseEntity<ChargePlanResponse> cobrar(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ChargePlanRequest request) throws Exception {
        UUID userId = SecurityUtils.currentUserId();
        Optional<IdempotencyService.StoredResponse> cached =
                idempotencyService.buscar(userId, CHARGE_ENDPOINT, idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.status(cached.get().statusCode())
                    .body(objectMapper.readValue(cached.get().responseBody(), ChargePlanResponse.class));
        }

        ChargePlanResponse charge = paymentService.cobrarPlano(userId, request);
        ResponseEntity<ChargePlanResponse> response = ResponseEntity.ok(charge);
        idempotencyService.salvar(userId, CHARGE_ENDPOINT, idempotencyKey, response);
        return response;
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionStatusResponse> assinatura() {
        return ResponseEntity.ok(paymentService.obterStatusAssinatura(SecurityUtils.currentUserId()));
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<SubscriptionStatusResponse> cancelarAssinatura() {
        paymentService.cancelarAssinatura(SecurityUtils.currentUserId());
        return ResponseEntity.ok(paymentService.obterStatusAssinatura(SecurityUtils.currentUserId()));
    }

    @PostMapping("/subscription/reactivate")
    public ResponseEntity<SubscriptionStatusResponse> reativarAssinatura() {
        paymentService.reativarAssinatura(SecurityUtils.currentUserId());
        return ResponseEntity.ok(paymentService.obterStatusAssinatura(SecurityUtils.currentUserId()));
    }

    @PostMapping("/mercadopago/webhook")
    public ResponseEntity<Void> webhookPost(@RequestParam(value = "topic", required = false) String topic,
                                            @RequestParam(value = "id", required = false) String id,
                                            @RequestParam(value = "type", required = false) String type,
                                            @RequestParam(value = "data.id", required = false) String dataId,
                                            HttpServletRequest request) {
        String paymentId = id != null ? id : dataId;
        if (!webhookVerifier.verificar(request, paymentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        processar(topic != null ? topic : type, paymentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mercadopago/webhook")
    public ResponseEntity<Void> webhookGet(@RequestParam(value = "topic", required = false) String topic,
                                           @RequestParam(value = "id", required = false) String id,
                                           HttpServletRequest request) {
        if (!webhookVerifier.verificar(request, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        processar(topic, id);
        return ResponseEntity.ok().build();
    }

    private void processar(String topic, String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        if ("payment".equalsIgnoreCase(topic) || "merchant_order".equalsIgnoreCase(topic)) {
            paymentService.processarNotificacao(id);
        }
    }
}
