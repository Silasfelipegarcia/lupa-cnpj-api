package br.com.lupainsights.repository;

import br.com.lupainsights.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    Optional<IdempotencyKeyEntity> findByIdAndUserIdAndEndpoint(String id, UUID userId, String endpoint);

    @Modifying
    @Query("DELETE FROM IdempotencyKeyEntity e WHERE e.expiresAt < :agora")
    int removerExpirados(Instant agora);
}
