package br.com.lupainsights.repository;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE u.autoRenew = true
              AND u.planCancelledAt IS NULL
              AND u.planValidUntil IS NOT NULL
              AND u.planValidUntil <= :limite
              AND u.defaultCardId IS NOT NULL
              AND u.plan IN (br.com.lupainsights.domain.SubscriptionPlan.PREMIUM,
                             br.com.lupainsights.domain.SubscriptionPlan.PRO_PLUS)
            """)
    List<UserEntity> findDueForRenewal(@Param("limite") Instant limite);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE u.trialAte IS NOT NULL
              AND u.trialAte <= :now
              AND u.plan = br.com.lupainsights.domain.SubscriptionPlan.PREMIUM
              AND (u.planValidUntil IS NULL OR u.planValidUntil <= :now)
            """)
    List<UserEntity> findTrialsExpirados(@Param("now") Instant now);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.role <> :adminRole")
    long countExcludingRole(@Param("adminRole") UserRole adminRole);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.role <> :adminRole AND u.createdAt >= :since")
    long countCreatedSinceExcludingRole(@Param("adminRole") UserRole adminRole, @Param("since") Instant since);

    @Query("SELECT u.plan, COUNT(u) FROM UserEntity u WHERE u.role <> :adminRole GROUP BY u.plan")
    List<Object[]> countByPlanExcludingRole(@Param("adminRole") UserRole adminRole);

    @Query("""
            SELECT COUNT(u) FROM UserEntity u
            WHERE u.role <> :adminRole
              AND u.trialAte IS NOT NULL
              AND u.trialAte > :now
            """)
    long countActiveTrialsExcludingRole(@Param("adminRole") UserRole adminRole, @Param("now") Instant now);

    @Query("""
            SELECT COUNT(u) FROM UserEntity u
            WHERE u.role <> :adminRole
              AND u.plan IN (br.com.lupainsights.domain.SubscriptionPlan.PREMIUM,
                             br.com.lupainsights.domain.SubscriptionPlan.PRO_PLUS)
              AND u.planCancelledAt IS NULL
              AND (u.planValidUntil IS NULL OR u.planValidUntil > :now)
            """)
    long countActivePaidSubscriptionsExcludingRole(@Param("adminRole") UserRole adminRole, @Param("now") Instant now);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE (:plan IS NULL OR u.plan = :plan)
              AND (
                :q IS NULL OR :q = ''
                OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY u.createdAt DESC
            """)
    Page<UserEntity> searchForAdmin(@Param("plan") SubscriptionPlan plan,
                                    @Param("q") String q,
                                    Pageable pageable);
}
