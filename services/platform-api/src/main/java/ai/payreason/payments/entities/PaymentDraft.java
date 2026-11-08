package ai.payreason.payments.entities;

import ai.payreason.payments.dto.CreatePaymentRequest;
import ai.payreason.payments.enums.PaymentChannel;
import ai.payreason.payments.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "payment_drafts")
public class PaymentDraft {
    @Id private UUID id;
    @Column(nullable = false, length = 80) private String reference;
    @Column(length = 80) private String customerReference;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private PaymentChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(nullable = false) private OffsetDateTime createdAt;
    @Column(nullable = false) private OffsetDateTime updatedAt;
    @Version private long version;
    @Column(length = 64, unique = true) private String paypalOrderId;
    @Column(length = 64) private String authorizationId;
    @Column(length = 64) private String captureId;
    @Column(length = 1000) private String approvalUrl;

    protected PaymentDraft() {}

    public PaymentDraft(UUID id, CreatePaymentRequest request, OffsetDateTime now) {
        this.id = id;
        reference = "PR-" + id.toString().replace("-", "").toUpperCase(Locale.ROOT);
        customerReference = normalize(request.customerReference());
        amount = request.amount();
        currency = request.currency();
        channel = request.channel();
        status = PaymentStatus.DRAFT;
        createdAt = now;
        updatedAt = now;
    }

    public boolean matches(CreatePaymentRequest request) {
        return java.util.Objects.equals(customerReference, normalize(request.customerReference()))
                && amount.compareTo(request.amount()) == 0 && currency.equals(request.currency())
                && channel == request.channel();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public void cancel(OffsetDateTime now) {
        status = PaymentStatus.CANCELLED;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getCustomerReference() { return customerReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentChannel getChannel() { return channel; }
    public PaymentStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getPaypalOrderId() { return paypalOrderId; }
    public String getAuthorizationId() { return authorizationId; }
    public String getCaptureId() { return captureId; }
    public String getApprovalUrl() { return approvalUrl; }
    public void order(String id, String approval, OffsetDateTime now) {
        paypalOrderId = id; approvalUrl = approval; transition(PaymentStatus.ORDER_CREATED, now);
    }
    public void authorize(String id, OffsetDateTime now) { authorizationId = id; transition(PaymentStatus.AUTHORIZED, now); }
    public void capture(String id, boolean completed, OffsetDateTime now) {
        captureId = id; transition(completed ? PaymentStatus.CAPTURED : PaymentStatus.CAPTURE_PENDING, now);
    }
    public void transition(PaymentStatus next, OffsetDateTime now) { status = next; updatedAt = now; }
}
