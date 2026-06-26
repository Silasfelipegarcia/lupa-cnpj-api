package br.com.lupainsights.repository;

import br.com.lupainsights.entity.PaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, UUID> {

    Optional<PaymentOrderEntity> findByMpPaymentId(String mpPaymentId);

    Optional<PaymentOrderEntity> findByMpPreferenceId(String mpPreferenceId);

    List<PaymentOrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<PaymentOrderEntity> findTop3ByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM PaymentOrderEntity p WHERE p.status = 'APPROVED'")
    long sumApprovedAmountAllTime();

    @Query("""
            SELECT COALESCE(SUM(p.amountCents), 0) FROM PaymentOrderEntity p
            WHERE p.status = 'APPROVED' AND p.paidAt >= :since
            """)
    long sumApprovedAmountSince(@Param("since") Instant since);

    @Query("""
            SELECT COUNT(p) FROM PaymentOrderEntity p
            WHERE p.status = 'APPROVED' AND p.paidAt >= :since
            """)
    long countApprovedSince(@Param("since") Instant since);

    @Query("SELECT COUNT(p) FROM PaymentOrderEntity p WHERE p.status NOT IN ('APPROVED', 'CANCELLED', 'REJECTED')")
    long countPendingPayments();

    @Query("""
            SELECT p.userId, COALESCE(SUM(p.amountCents), 0) FROM PaymentOrderEntity p
            WHERE p.status = 'APPROVED' AND p.userId IN :userIds
            GROUP BY p.userId
            """)
    List<Object[]> sumApprovedAmountByUserIds(@Param("userIds") List<UUID> userIds);

    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM PaymentOrderEntity p WHERE p.status = 'APPROVED' AND p.userId = :userId")
    long sumApprovedAmountByUserId(@Param("userId") UUID userId);
}
