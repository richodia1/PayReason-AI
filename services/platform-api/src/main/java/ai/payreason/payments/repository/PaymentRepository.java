package ai.payreason.payments.repository;

import ai.payreason.payments.entities.PaymentDraft;
import ai.payreason.payments.enums.PaymentStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentDraft, UUID>, JpaSpecificationExecutor<PaymentDraft> {
    long countByStatus(PaymentStatus status);
    Page<PaymentDraft> findByStatus(PaymentStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentDraft p where p.id = :id")
    Optional<PaymentDraft> lockById(UUID id);
    Optional<PaymentDraft> findByPaypalOrderId(String paypalOrderId);
}
