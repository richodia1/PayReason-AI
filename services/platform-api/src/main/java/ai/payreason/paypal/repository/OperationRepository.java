package ai.payreason.paypal.repository;

import ai.payreason.paypal.entities.PaypalOperation;
import ai.payreason.paypal.enums.OperationKind;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationRepository extends JpaRepository<PaypalOperation, UUID> {
    Optional<PaypalOperation> findByPaymentIdAndKind(UUID paymentId, OperationKind kind);
    List<PaypalOperation> findByPaymentId(UUID paymentId);
    boolean existsByPaymentIdAndState(UUID paymentId, String state);
}
