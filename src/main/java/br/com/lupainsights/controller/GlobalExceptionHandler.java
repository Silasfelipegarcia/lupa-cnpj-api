package br.com.lupainsights.controller;

import br.com.lupainsights.exception.ForbiddenException;
import br.com.lupainsights.exception.TooManyRequestsException;
import br.com.lupainsights.observability.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(respostaErro(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(respostaErro("Acesso negado"));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotAuthenticated(AuthenticationCredentialsNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respostaErro("Não autenticado"));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, String>> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(respostaErro(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(respostaErro(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(respostaErro(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(respostaErro("Rota não encontrada"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Erro não tratado requestId={} userId={} message={}",
                RequestContext.requestIdAtual(),
                org.slf4j.MDC.get(RequestContext.MDC_USER_ID),
                ex.getMessage(),
                ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(respostaErro("Erro interno no servidor. Tente novamente mais tarde."));
    }

    private Map<String, String> respostaErro(String mensagem) {
        String requestId = RequestContext.requestIdAtual();
        if (requestId != null && !requestId.isBlank()) {
            return Map.of("erro", mensagem, "requestId", requestId);
        }
        return Map.of("erro", mensagem);
    }
}
