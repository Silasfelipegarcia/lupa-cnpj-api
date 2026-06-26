package br.com.lupainsights.security;

import br.com.lupainsights.config.SecurityProperties;
import br.com.lupainsights.util.IpRateLimiter;
import br.com.lupainsights.util.RequestIpResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String CLIENT_IP_ATTRIBUTE = "clientIp";

    private static final long HOUR_MS = 60L * 60 * 1000;
    private static final long MINUTE_MS = 60L * 1000;

    private final SecurityProperties securityProperties;
    private final IpRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(SecurityProperties securityProperties,
                           IpRateLimiter rateLimiter,
                           ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = PublicApiRoutes.normalizePath(request.getRequestURI());
        return path.startsWith("/actuator") || path.equals("/health") || path.equals("/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = PublicApiRoutes.normalizePath(request.getRequestURI());
        String method = request.getMethod();
        String clientIp = resolverClientIp(request);
        request.setAttribute(CLIENT_IP_ATTRIBUTE, clientIp);

        RateLimitRule rule = resolverRegraIp(path, method);
        if (rule != null) {
            String key = "ip:" + clientIp + ":" + rule.suffix();
            if (!rateLimiter.tryAcquire(key, rule.maxRequests(), rule.windowMs())) {
                responder429(response, rule.retryAfterSeconds());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    static String resolverClientIp(HttpServletRequest request) {
        Object atributo = request.getAttribute(CLIENT_IP_ATTRIBUTE);
        if (atributo instanceof String ip && !ip.isBlank()) {
            return ip;
        }
        return RequestIpResolver.resolver(request);
    }

    private RateLimitRule resolverRegraIp(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && path.equals("/cnpj/import")) {
            return new RateLimitRule(securityProperties.getImportPerHour(), HOUR_MS, 3600, "import");
        }
        if ("GET".equalsIgnoreCase(method) && path.equals("/cnpj/template")) {
            return new RateLimitRule(securityProperties.getTemplatePerHour(), HOUR_MS, 3600, "template");
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+/status")) {
            return new RateLimitRule(securityProperties.getStatusPerMinute(), MINUTE_MS, 60, "status");
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+/download")) {
            return new RateLimitRule(securityProperties.getDownloadPerHour(), HOUR_MS, 3600, "download");
        }
        if ("DELETE".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+")) {
            return new RateLimitRule(securityProperties.getStatusPerMinute(), MINUTE_MS, 60, "cancel");
        }
        if ("POST".equalsIgnoreCase(method) && path.matches("/auth/(login|register)")) {
            return new RateLimitRule(securityProperties.getAuthPerMinute(), MINUTE_MS, 60, "auth");
        }
        if ("PUT".equalsIgnoreCase(method) && path.equals("/auth/password")) {
            return new RateLimitRule(securityProperties.getPasswordChangePerHour(), HOUR_MS, 3600, "password");
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/cnpj/preview")) {
            return new RateLimitRule(securityProperties.getGuestPreviewPerMinute(), MINUTE_MS, 60, "preview");
        }
        if (path.startsWith("/payments") && !path.contains("/mercadopago/webhook")) {
            return new RateLimitRule(securityProperties.getPaymentsPerMinute(), MINUTE_MS, 60, "payments");
        }
        if (path.startsWith("/analytics")) {
            return new RateLimitRule(securityProperties.getAnalyticsPerMinute(), MINUTE_MS, 60, "analytics");
        }
        return null;
    }

    private void responder429(HttpServletResponse response, int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "erro", "Muitas requisições. Aguarde alguns instantes e tente novamente."
        ));
    }

    record RateLimitRule(int maxRequests, long windowMs, int retryAfterSeconds, String suffix) {
    }
}
