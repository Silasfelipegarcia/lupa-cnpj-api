package br.com.lupainsights.security;

import br.com.lupainsights.exception.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID currentUserId() {
        return currentPrincipal().getId();
    }

    public static UUID currentUserIdOrNull() {
        UserPrincipal principal = currentPrincipalOrNull();
        return principal != null ? principal.getId() : null;
    }

    public static boolean isAdmin() {
        UserPrincipal principal = currentPrincipalOrNull();
        return principal != null && principal.isAdmin();
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("Acesso restrito a administradores.");
        }
    }

    public static UserPrincipal currentPrincipal() {
        UserPrincipal principal = currentPrincipalOrNull();
        if (principal == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return principal;
    }

    private static UserPrincipal currentPrincipalOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        return null;
    }
}
