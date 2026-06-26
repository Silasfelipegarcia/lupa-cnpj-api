package br.com.lupainsights.payment;

import br.com.lupainsights.entity.IdempotencyKeyEntity;
import br.com.lupainsights.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final int TTL_HORAS = 24;

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<StoredResponse> buscar(UUID userId, String endpoint, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIdAndUserIdAndEndpoint(idempotencyKey.trim(), userId, endpoint)
                .filter(entry -> entry.getExpiresAt().isAfter(Instant.now()))
                .map(entry -> new StoredResponse(entry.getStatusCode(), entry.getResponseBody()));
    }

    @Transactional
    public void salvar(UUID userId, String endpoint, String idempotencyKey,
                       ResponseEntity<?> response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(response.getBody());
            IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
            entity.setId(idempotencyKey.trim());
            entity.setUserId(userId);
            entity.setEndpoint(endpoint);
            entity.setResponseBody(body);
            entity.setStatusCode(response.getStatusCode().value());
            entity.setCreatedAt(Instant.now());
            entity.setExpiresAt(Instant.now().plus(TTL_HORAS, ChronoUnit.HOURS));
            repository.save(entity);
        } catch (Exception ignored) {
            // Idempotência é best-effort; não falha a operação principal
        }
    }

    @Transactional
    public void limparExpirados() {
        repository.removerExpirados(Instant.now());
    }

    public record StoredResponse(int statusCode, String responseBody) {
    }
}
