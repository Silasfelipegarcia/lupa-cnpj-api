package br.com.dadoscnpj.security;

import br.com.dadoscnpj.config.SecurityProperties;
import br.com.dadoscnpj.util.IpRateLimiter;
import br.com.dadoscnpj.util.RequestIpResolver;
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
@Order(1)
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/cnpj")) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = RequestIpResolver.resolver(request);
        request.setAttribute(CLIENT_IP_ATTRIBUTE, clientIp);

        String rateLimitKey = montarChave(clientIp, path, request.getMethod());
        RateLimitRule rule = resolverRegra(path, request.getMethod());

        if (rule != null && !rateLimiter.tryAcquire(rateLimitKey, rule.maxRequests(), rule.windowMs())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(rule.retryAfterSeconds()));
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "erro", "Muitas requisições. Aguarde alguns instantes e tente novamente."
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolverRegra(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && path.equals("/cnpj/import")) {
            return new RateLimitRule(securityProperties.getImportPerHour(), HOUR_MS, 3600);
        }
        if ("GET".equalsIgnoreCase(method) && path.equals("/cnpj/template")) {
            return new RateLimitRule(securityProperties.getTemplatePerHour(), HOUR_MS, 3600);
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+/status")) {
            return new RateLimitRule(securityProperties.getStatusPerMinute(), MINUTE_MS, 60);
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+/download")) {
            return new RateLimitRule(securityProperties.getDownloadPerHour(), HOUR_MS, 3600);
        }
        return null;
    }

    private String montarChave(String clientIp, String path, String method) {
        if (path.matches("/cnpj/import/[^/]+/status")) {
            return clientIp + ":status";
        }
        if (path.matches("/cnpj/import/[^/]+/download")) {
            return clientIp + ":download";
        }
        return clientIp + ":" + method + ":" + path;
    }

    private record RateLimitRule(int maxRequests, long windowMs, int retryAfterSeconds) {
    }
}
