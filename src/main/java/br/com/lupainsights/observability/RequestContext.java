package br.com.lupainsights.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.UUID;

public final class RequestContext {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String REQUEST_ATTRIBUTE = "requestId";

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CLIENT_IP = "clientIp";

    private RequestContext() {
    }

    public static String resolverOuGerarRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER_REQUEST_ID);
        if (incoming != null && !incoming.isBlank() && incoming.length() <= 64) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }

    public static void iniciar(String requestId, String clientIp) {
        MDC.put(MDC_REQUEST_ID, requestId);
        if (clientIp != null) {
            MDC.put(MDC_CLIENT_IP, clientIp);
        }
    }

    public static void definirUserId(String userId) {
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId);
        }
    }

    public static void limpar() {
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_CLIENT_IP);
    }

    public static String requestIdAtual() {
        return MDC.get(MDC_REQUEST_ID);
    }
}
