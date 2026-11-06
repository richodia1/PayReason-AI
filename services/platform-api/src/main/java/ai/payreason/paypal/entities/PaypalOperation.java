package ai.payreason.paypal.entities;

import ai.payreason.paypal.enums.OperationKind;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "paypal_operations")
public class PaypalOperation {
    @Id private UUID id;
    @Column(nullable = false) private UUID paymentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private OperationKind kind;
    @Column(nullable = false, length = 16) private String state;
    @Column(nullable = false) private OffsetDateTime startedAt;
    private OffsetDateTime leasedUntil;
    protected PaypalOperation() {}
    public PaypalOperation(UUID paymentId, OperationKind kind, OffsetDateTime now) {
        id = UUID.randomUUID(); this.paymentId = paymentId; this.kind = kind; startedAt = now; state = "PENDING";
    }
    public void lease(OffsetDateTime now) { state = "PENDING"; leasedUntil = now.plusSeconds(45); }
    public void complete() { state = "SUCCEEDED"; leasedUntil = null; }
    public void failed(boolean uncertain) { state = uncertain ? "PENDING" : "FAILED"; leasedUntil = null; }
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public OperationKind getKind() { return kind; }
    public String getState() { return state; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public boolean leased(OffsetDateTime now) { return leasedUntil != null && leasedUntil.isAfter(now); }
}
