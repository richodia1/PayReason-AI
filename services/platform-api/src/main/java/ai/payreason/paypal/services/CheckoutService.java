package ai.payreason.paypal.services;

import ai.payreason.audit.entities.PaymentActivity;
import ai.payreason.audit.repository.PaymentActivityRepository;
import ai.payreason.payments.dto.PaymentResponse;
import ai.payreason.payments.entities.PaymentDraft;
import ai.payreason.payments.enums.PaymentStatus;
import ai.payreason.payments.repository.PaymentRepository;
import ai.payreason.paypal.entities.PaypalOperation;
import ai.payreason.paypal.enums.OperationKind;
import ai.payreason.paypal.repository.OperationRepository;
import ai.payreason.risk.enums.ReviewDecision;
import ai.payreason.risk.repository.RiskRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class CheckoutService {
    private final PaymentRepository payments;
    private final OperationRepository operations;
    private final RiskRepository risks;
    private final PaymentActivityRepository activities;
    private final PaypalClient paypal;
    private final Clock clock;
    private final TransactionTemplate tx;
    public CheckoutService(PaymentRepository payments, OperationRepository operations, RiskRepository risks,
            PaymentActivityRepository activities, PaypalClient paypal, Clock clock, PlatformTransactionManager manager) {
        this.payments = payments; this.operations = operations; this.risks = risks; this.activities = activities;
        this.paypal = paypal; this.clock = clock; tx = new TransactionTemplate(manager);
    }
    private record Work(PaymentDraft payment, UUID operationId) {}
    public record OperationView(UUID id, OperationKind kind, String state, OffsetDateTime startedAt) {}
    public List<OperationView> operations(UUID id) {
        if (!payments.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found.");
        return operations.findByPaymentId(id).stream().map(o -> new OperationView(o.getId(), o.getKind(), o.getState(), o.getStartedAt())).toList();
    }

    public PaymentResponse run(UUID id, OperationKind kind, String actor) {
        paypal.requireConfigured();
        // Commit the request identity before the network call so crashes retain the same retry key.
        Work work = tx.execute(status -> prepare(id, kind));
        if (work == null) return PaymentResponse.from(payments.findById(id).orElseThrow());
        try {
            JsonNode result = switch (kind) {
                case CREATE -> paypal.create(work.payment(), work.operationId());
                case AUTHORIZE -> paypal.authorize(work.payment().getPaypalOrderId(), work.operationId());
                case CAPTURE -> paypal.capture(work.payment().getAuthorizationId(), work.operationId());
                case VOID -> { paypal.voidAuthorization(work.payment().getAuthorizationId(), work.operationId()); yield null; }
            };
            return tx.execute(status -> {
                var p = lock(id);
                var operation = operations.findById(work.operationId()).orElseThrow();
                apply(p, kind, result);
                operation.complete();
                activities.save(new PaymentActivity(id, "PAYPAL_" + kind.name(), actor, now()));
                return PaymentResponse.from(p);
            });
        } catch (RuntimeException exception) {
            tx.executeWithoutResult(status -> {
                lock(id);
                // Even provider errors can describe an already-applied operation. Do not release the review gate.
                operations.findById(work.operationId()).ifPresent(op -> op.failed(true));
            });
            throw exception;
        }
    }

    private Work prepare(UUID id, OperationKind kind) {
        var p = lock(id);
        var prior = operations.findByPaymentIdAndKind(id, kind);
        if (prior.isPresent() && prior.get().getState().equals("SUCCEEDED")) return null;
        if (operations.findByPaymentId(id).stream().anyMatch(op -> op.getState().equals("PENDING") && op.getKind() != kind))
            throw conflict("Another PayPal operation is unresolved. Reconcile or retry that operation first.");
        var required = switch (kind) {
            case CREATE -> PaymentStatus.DRAFT;
            case AUTHORIZE -> PaymentStatus.ORDER_CREATED;
            case CAPTURE, VOID -> PaymentStatus.AUTHORIZED;
        };
        if (p.getStatus() != required) throw conflict("Payment is not in the required state: " + required);
        if (kind == OperationKind.CAPTURE || kind == OperationKind.VOID) {
            var risk = risks.findById(id).orElseThrow(() -> conflict("Assess and review this payment first."));
            var decision = kind == OperationKind.CAPTURE ? ReviewDecision.RELEASE : ReviewDecision.BLOCK;
            if (risk.getDecision() != decision) throw conflict("Reviewer decision must be " + decision + " before " + kind + ".");
        }
        var op = prior.orElseGet(() -> new PaypalOperation(id, kind, now()));
        if (op.leased(now())) throw conflict("This operation is already running. Wait before retrying.");
        // Stay within the provider's default order idempotency window. Never silently issue a fresh key.
        if (op.getStartedAt().isBefore(now().minusHours(5)))
            throw conflict("Retry window expired. Reconcile with PayPal; manual recovery is required if no resource ID was received.");
        op.lease(now()); operations.save(op);
        return new Work(p, op.getId());
    }

    private void apply(PaymentDraft p, OperationKind kind, JsonNode result) {
        switch (kind) {
            case CREATE -> p.order(PaypalClient.id(result.path("id").asText()), PaypalClient.approval(result), now());
            case AUTHORIZE -> {
                validateOrder(p, result);
                var authorization = result.path("purchase_units").path(0).path("payments").path("authorizations").path(0);
                validateAmount(p, authorization.path("amount"));
                if (!"CREATED".equals(authorization.path("status").asText())) throw conflict("PayPal authorization is not ready. Reconcile before continuing.");
                p.authorize(PaypalClient.id(authorization.path("id").asText()), now());
            }
            case CAPTURE -> applyCapture(p, result);
            case VOID -> p.transition(PaymentStatus.VOIDED, now());
        }
    }

    public PaymentResponse reconcile(UUID id, String actor) {
        return tx.execute(status -> {
            var p = lock(id);
            if (operations.findByPaymentId(id).stream().anyMatch(op -> op.leased(now())))
                throw conflict("An operation is still running. Wait before reconciling.");
            if (p.getPaypalOrderId() == null) throw conflict("No PayPal order ID is available. Retry order creation with the existing operation key.");
            var order = paypal.order(p.getPaypalOrderId());
            validateOrder(p, order);
            var providerPayments = order.path("purchase_units").path(0).path("payments");
            var auth = providerPayments.path("authorizations").path(0);
            if (!auth.isMissingNode()) {
                validateAmount(p, auth.path("amount"));
                String authId = PaypalClient.id(auth.path("id").asText());
                if (p.getAuthorizationId() == null) p.authorize(authId, now());
                if (!authId.equals(p.getAuthorizationId())) throw conflict("PayPal authorization does not match the stored payment.");
                var current = paypal.authorization(authId);
                validateAmount(p, current.path("amount"));
                if ("VOIDED".equals(current.path("status").asText()) && p.getCaptureId() == null)
                    p.transition(PaymentStatus.VOIDED, now());
            }
            var capture = providerPayments.path("captures").path(0);
            if (p.getCaptureId() != null) capture = paypal.captured(p.getCaptureId());
            if (!capture.isMissingNode()) applyCapture(p, capture);
            for (var op : operations.findByPaymentId(id)) {
                boolean completed = switch (op.getKind()) {
                    case CREATE -> p.getPaypalOrderId() != null;
                    case AUTHORIZE -> p.getAuthorizationId() != null;
                    case CAPTURE -> p.getCaptureId() != null;
                    case VOID -> p.getStatus() == PaymentStatus.VOIDED;
                };
                if (completed) op.complete();
            }
            activities.save(new PaymentActivity(id, "PAYPAL_RECONCILED", actor, now()));
            return PaymentResponse.from(p);
        });
    }

    private void applyCapture(PaymentDraft p, JsonNode capture) {
        validateAmount(p, capture.path("amount"));
        String captureId = PaypalClient.id(capture.path("id").asText());
        if (p.getCaptureId() != null && !p.getCaptureId().equals(captureId)) throw conflict("PayPal capture does not match this payment.");
        String state = capture.path("status").asText();
        if (!Set.of("COMPLETED", "PENDING", "DECLINED", "FAILED").contains(state))
            throw conflict("Unsupported PayPal capture state; inspect the Sandbox account before proceeding.");
        p.capture(captureId, state.equals("COMPLETED"), now());
        if (Set.of("DECLINED", "FAILED").contains(state)) p.transition(PaymentStatus.DECLINED, now());
    }
    private void validateOrder(PaymentDraft p, JsonNode order) {
        if (!p.getPaypalOrderId().equals(order.path("id").asText()) || !"AUTHORIZE".equals(order.path("intent").asText())
                || order.path("purchase_units").size() != 1 ||
                !p.getId().toString().equals(order.path("purchase_units").path(0).path("reference_id").asText()))
            throw conflict("PayPal order does not match the stored payment.");
        validateAmount(p, order.path("purchase_units").path(0).path("amount"));
    }
    private void validateAmount(PaymentDraft p, JsonNode amount) {
        try {
            if (!p.getCurrency().equals(amount.path("currency_code").asText()) ||
                    p.getAmount().compareTo(new BigDecimal(amount.path("value").asText())) != 0) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) { throw conflict("PayPal amount does not match the stored payment."); }
    }
    private PaymentDraft lock(UUID id) { return payments.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found.")); }
    private OffsetDateTime now() { return OffsetDateTime.now(clock); }
    private ResponseStatusException conflict(String detail) { return new ResponseStatusException(HttpStatus.CONFLICT, detail); }
}
