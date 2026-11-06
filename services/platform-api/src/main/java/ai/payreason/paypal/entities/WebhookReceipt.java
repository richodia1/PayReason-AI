package ai.payreason.paypal.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "paypal_webhooks")
public class WebhookReceipt {
    @Id @Column(length = 100) private String id;
    @Column(nullable = false, length = 100) private String eventType;
    @Column(nullable = false) private OffsetDateTime receivedAt;
    protected WebhookReceipt() {}
    public WebhookReceipt(String id, String type, OffsetDateTime now) { this.id = id; eventType = type; receivedAt = now; }
}
