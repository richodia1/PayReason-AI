package ai.payreason.audit.repository;

import ai.payreason.audit.entities.PaymentActivity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentActivityRepository extends JpaRepository<PaymentActivity, UUID> {
    List<PaymentActivity> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}
