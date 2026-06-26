package br.com.lupainsights.repository;

import br.com.lupainsights.entity.ImportJobEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    @Query("SELECT COUNT(j) FROM ImportJobEntity j WHERE j.userId = :userId AND j.createdAt >= :inicio")
    long countByUserIdSince(@Param("userId") UUID userId, @Param("inicio") Instant inicio);

    @Query("SELECT COUNT(j) FROM ImportJobEntity j")
    long countAllJobs();

    @Query("SELECT COALESCE(SUM(j.processados), 0) FROM ImportJobEntity j")
    long sumProcessadosAll();

    @Query("SELECT COALESCE(SUM(j.sucesso), 0) FROM ImportJobEntity j")
    long sumSucessoAll();

    @Query("SELECT COALESCE(SUM(j.erros), 0) FROM ImportJobEntity j")
    long sumErrosAll();

    @Query("""
            SELECT j.userId, COUNT(j), COALESCE(SUM(j.processados), 0)
            FROM ImportJobEntity j WHERE j.userId IN :userIds GROUP BY j.userId
            """)
    List<Object[]> metricsByUserIds(@Param("userIds") List<UUID> userIds);

    @Query("SELECT COUNT(j) FROM ImportJobEntity j WHERE j.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(j.processados), 0) FROM ImportJobEntity j WHERE j.userId = :userId")
    long sumProcessadosByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(j.sucesso), 0) FROM ImportJobEntity j WHERE j.userId = :userId")
    long sumSucessoByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(j.erros), 0) FROM ImportJobEntity j WHERE j.userId = :userId")
    long sumErrosByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COALESCE(SUM(j.processados), 0) FROM ImportJobEntity j
            WHERE j.userId = :userId AND j.createdAt >= :since
            """)
    long sumProcessadosByUserIdSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
