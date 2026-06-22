package br.com.dadoscnpj.repository;

import br.com.dadoscnpj.entity.ImportJobEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, UUID> {

    List<ImportJobEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ImportJobEntity> findByUserIdAndListaSalvaTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<ImportJobEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT COUNT(j) FROM ImportJobEntity j WHERE j.userId = :userId AND j.status IN ('NA_FILA', 'PROCESSANDO')")
    long countAtivosPorUsuario(@Param("userId") UUID userId);

    @Query("SELECT COUNT(j) FROM ImportJobEntity j WHERE j.status IN ('NA_FILA', 'PROCESSANDO')")
    long countAtivos();

    @Query("SELECT j FROM ImportJobEntity j WHERE j.status IN ('NA_FILA', 'PROCESSANDO') ORDER BY j.createdAt ASC")
    List<ImportJobEntity> findAtivosOrderByCreatedAtAsc();

    Optional<ImportJobEntity> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
            UUID userId, List<String> statuses);
}
