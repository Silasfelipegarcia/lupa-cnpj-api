package br.com.lupainsights.security;

import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = PublicApiRoutes.normalizePath(request.getRequestURI());
        String method = request.getMethod();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                UUID userId = jwtService.extrairUserId(token);
                String email = jwtService.validarToken(token).get("email", String.class);
                UserEntity user = userRepository.findById(userId)
                        .orElseThrow(() -> new JwtException("Usuário não encontrado"));
                UserPrincipal principal = new UserPrincipal(
                        user.getId(), user.getEmail(), user.getRole(), user.getPlan());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                if (exigeJwt(path, method)) {
                    responderNaoAutorizado(response, "Token inválido ou expirado");
                    return;
                }
            }
        } else if (exigeJwt(path, method)) {
            responderNaoAutorizado(response, "Token JWT obrigatório");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean exigeJwt(String path, String method) {
        return !PublicApiRoutes.isPublic(path, method);
    }

    private void responderNaoAutorizado(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
