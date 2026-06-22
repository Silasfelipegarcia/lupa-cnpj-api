package br.com.dadoscnpj.repository;

import br.com.dadoscnpj.entity.PaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, UUID> {

    Optional<PaymentOrderEntity> findByMpPaymentId(String mpPaymentId);

    Optional<PaymentOrderEntity> findByMpPreferenceId(String mpPreferenceId);

    List<PaymentOrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
