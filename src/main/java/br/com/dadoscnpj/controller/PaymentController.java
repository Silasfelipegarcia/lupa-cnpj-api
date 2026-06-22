package br.com.dadoscnpj.controller;

import br.com.dadoscnpj.dto.CheckoutRequest;
import br.com.dadoscnpj.dto.CheckoutResponse;
import br.com.dadoscnpj.dto.PaymentHistoryItemResponse;
import br.com.dadoscnpj.payment.MercadoPagoPaymentService;
import br.com.dadoscnpj.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final MercadoPagoPaymentService paymentService;

    public PaymentController(MercadoPagoPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(paymentService.criarCheckout(
                SecurityUtils.currentUserId(),
                request.getPlan()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryItemResponse>> historico() {
        return ResponseEntity.ok(paymentService.listarHistorico(SecurityUtils.currentUserId()));
    }

    @PostMapping("/mercadopago/webhook")
    public ResponseEntity<Void> webhookPost(@RequestParam(value = "topic", required = false) String topic,
                                            @RequestParam(value = "id", required = false) String id,
                                            @RequestParam(value = "type", required = false) String type,
                                            @RequestParam(value = "data.id", required = false) String dataId) {
        processar(topic != null ? topic : type, id != null ? id : dataId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mercadopago/webhook")
    public ResponseEntity<Void> webhookGet(@RequestParam(value = "topic", required = false) String topic,
                                           @RequestParam(value = "id", required = false) String id) {
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
