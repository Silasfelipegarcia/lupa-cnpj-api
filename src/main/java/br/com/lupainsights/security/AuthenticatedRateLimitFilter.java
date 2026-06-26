package br.com.lupainsights.security;

import br.com.lupainsights.config.SecurityProperties;
import br.com.lupainsights.util.IpRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthenticatedRateLimitFilter extends OncePerRequestFilter {

    private static final long HOUR_MS = 60L * 60 * 1000;

    private final SecurityProperties securityProperties;
    private final IpRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public AuthenticatedRateLimitFilter(SecurityProperties securityProperties,
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
        UUID userId = SecurityUtils.currentUserIdOrNull();
        if (userId != null) {
            String path = PublicApiRoutes.normalizePath(request.getRequestURI());
            String method = request.getMethod();

            if ("POST".equalsIgnoreCase(method) && path.equals("/cnpj/import")) {
                String key = "user:" + userId + ":import";
                if (!rateLimiter.tryAcquire(key, securityProperties.getImportPerUserPerHour(), HOUR_MS)) {
                    responder429(response, 3600);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void responder429(HttpServletResponse response, int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "erro", "Muitas requisições para sua conta. Aguarde e tente novamente."
        ));
    }
}
