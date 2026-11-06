package ai.payreason.payments.services;

import ai.payreason.audit.entities.PaymentActivity;
import ai.payreason.audit.dto.ActivityResponse;
import ai.payreason.audit.repository.PaymentActivityRepository;
import ai.payreason.payments.dto.CreatePaymentRequest;
import ai.payreason.payments.dto.PaymentResponse;
import ai.payreason.payments.entities.PaymentDraft;
import ai.payreason.payments.enums.PaymentStatus;
import ai.payreason.payments.repository.PaymentRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentActivityRepository activity;
    private final Clock clock;
    private final ai.payreason.paypal.repository.OperationRepository operations;

    public PaymentService(PaymentRepository payments, PaymentActivityRepository activity, Clock clock,
            ai.payreason.paypal.repository.OperationRepository operations) {
        this.payments = payments;
        this.activity = activity;
        this.clock = clock;
        this.operations = operations;
    }

    public Page<PaymentResponse> list(int page, int size, PaymentStatus status) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return (status == null ? payments.findAll(pageable) : payments.findByStatus(status, pageable))
                .map(PaymentResponse::from);
    }

    public PaymentResponse get(UUID id) { return PaymentResponse.from(find(id)); }

    public Map<String, Long> summary() {
        return Map.of("total", payments.count(), "drafts", payments.countByStatus(PaymentStatus.DRAFT),
                "cancelled", payments.countByStatus(PaymentStatus.CANCELLED));
    }

    @Transactional
    public PaymentResponse create(UUID requestId, CreatePaymentRequest request, String actor) {
        var existing = payments.findById(requestId);
        if (existing.isPresent()) {
            if (!existing.get().matches(request)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This request key was used for different payment details.");
            }
            return PaymentResponse.from(existing.get());
        }
        var now = OffsetDateTime.now(clock);
        var payment = payments.save(new PaymentDraft(requestId, request, now));
        activity.save(new PaymentActivity(payment.getId(), "DRAFT_CREATED", actor, now));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse cancel(UUID id, String actor) {
        var payment = payments.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
        if (operations.existsByPaymentIdAndState(id, "PENDING"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reconcile the pending PayPal operation before cancelling.");
        if (payment.getStatus() == PaymentStatus.CANCELLED) return PaymentResponse.from(payment);
        if (payment.getStatus() != PaymentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only local drafts can be cancelled here.");
        }
        var now = OffsetDateTime.now(clock);
        payment.cancel(now);
        activity.save(new PaymentActivity(id, "DRAFT_CANCELLED", actor, now));
        return PaymentResponse.from(payment);
    }

    public List<ActivityResponse> activity(UUID id) {
        find(id);
        return activity.findByPaymentIdOrderByOccurredAtAsc(id).stream().map(ActivityResponse::from).toList();
    }

    private PaymentDraft find(UUID id) {
        return payments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
    }
}
