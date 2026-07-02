package br.com.lupainsights.security;

import org.springframework.http.HttpMethod;

public final class PublicApiRoutes {

    private PublicApiRoutes() {
    }

    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.startsWith("/api/")) {
            return path.substring(4);
        }
        if (path.equals("/api")) {
            return "/";
        }
        return path;
    }

    public static boolean isPublic(String path, String method) {
        String normalized = normalizePath(path);
        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }
        if ("/health".equals(normalized) || "/".equals(normalized) || normalized.startsWith("/actuator/health")) {
            return true;
        }
        if (normalized.startsWith("/auth/register") || normalized.startsWith("/auth/login")
                || normalized.startsWith("/auth/forgot-password") || normalized.startsWith("/auth/reset-password")
                || normalized.startsWith("/auth/bootstrap-admin")) {
            return true;
        }
        if (normalized.startsWith("/cnpj/preview")) {
            return true;
        }
        if (normalized.startsWith("/plans")) {
            return true;
        }
        if (normalized.startsWith("/payments/mercadopago/webhook")) {
            return true;
        }
        if (normalized.startsWith("/analytics/event")) {
            return true;
        }
        return false;
    }
}
