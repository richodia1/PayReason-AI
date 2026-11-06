package ai.payreason.audit.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_activity")
public class PaymentActivity {
    @Id private UUID id;
    @Column(nullable = false) private UUID paymentId;
    @Column(nullable = false, length = 32) private String action;
    @Column(nullable = false, length = 100) private String actor;
    @Column(nullable = false) private OffsetDateTime occurredAt;
    @Column(length = 2000) private String notes;

    protected PaymentActivity() {}

    public PaymentActivity(UUID paymentId, String action, String actor, OffsetDateTime now) {
        id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.action = action;
        this.actor = actor;
        occurredAt = now;
    }
    public PaymentActivity(UUID paymentId, String action, String actor, OffsetDateTime now, String notes) {
        this(paymentId, action, actor, now); this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public String getNotes() { return notes; }
}
