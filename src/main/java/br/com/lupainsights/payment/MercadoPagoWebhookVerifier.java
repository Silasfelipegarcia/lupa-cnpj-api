package br.com.lupainsights.payment;

import br.com.lupainsights.config.MercadoPagoProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MercadoPagoWebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookVerifier.class);

    private final MercadoPagoProperties properties;

    public MercadoPagoWebhookVerifier(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    public boolean verificar(HttpServletRequest request, String dataId) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            return true;
        }

        String signature = request.getHeader("x-signature");
        String requestId = request.getHeader("x-request-id");
        if (signature == null || requestId == null || dataId == null || dataId.isBlank()) {
            log.warn("Webhook MP rejeitado: headers ou data.id ausentes");
            return false;
        }

        Map<String, String> parts = java.util.Arrays.stream(signature.split(","))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1], (a, b) -> b));

        String ts = parts.get("ts");
        String v1 = parts.get("v1");
        if (ts == null || v1 == null) {
            log.warn("Webhook MP rejeitado: assinatura malformada");
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String expected = hmacSha256Hex(secret, manifest);
        boolean valido = expected.equalsIgnoreCase(v1);
        if (!valido) {
            log.warn("Webhook MP rejeitado: assinatura inválida para data.id={}", dataId);
        }
        return valido;
    }

    private static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao validar assinatura do webhook", e);
        }
    }
}
