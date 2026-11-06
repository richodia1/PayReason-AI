package ai.payreason.risk.services;

import ai.payreason.audit.entities.PaymentActivity;
import ai.payreason.audit.repository.PaymentActivityRepository;
import ai.payreason.payments.enums.PaymentStatus;
import ai.payreason.payments.repository.PaymentRepository;
import ai.payreason.paypal.repository.OperationRepository;
import ai.payreason.risk.dto.*;
import ai.payreason.risk.entities.RiskAssessment;
import ai.payreason.risk.repository.RiskRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class RiskService {
    private final RiskRepository risks;
    private final PaymentRepository payments;
    private final OperationRepository operations;
    private final PaymentActivityRepository activities;
    private final ScoringClient scorer;
    private final ObjectMapper json;
    private final Clock clock;
    public RiskService(RiskRepository risks, PaymentRepository payments, OperationRepository operations,
            PaymentActivityRepository activities, ScoringClient scorer, ObjectMapper json, Clock clock) {
        this.risks = risks; this.payments = payments; this.operations = operations; this.activities = activities;
        this.scorer = scorer; this.json = json; this.clock = clock;
    }
    public AssessmentResponse get(UUID id) {
        return AssessmentResponse.from(risks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No assessment yet.")), json);
    }
    @Transactional
    public AssessmentResponse assess(UUID id, RiskFeatures features, String actor) {
        var p = payments.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
        var existing = risks.findById(id);
        String input = json.writeValueAsString(features);
        if (existing.isPresent()) {
            if (!existing.get().getInputFeatures().equals(input)) throw conflict("An immutable assessment already exists. Create a new draft for a different scenario.");
            return AssessmentResponse.from(existing.get(), json);
        }
        if (!Set.of(PaymentStatus.DRAFT, PaymentStatus.ORDER_CREATED, PaymentStatus.AUTHORIZED).contains(p.getStatus()))
            throw conflict("Only active, uncaptured payments can be assessed.");
        var result = scorer.score(p, features);
        var now = OffsetDateTime.now(clock);
        var risk = risks.save(new RiskAssessment(id, result, input, actor, now));
        activities.save(new PaymentActivity(id, "RISK_ASSESSED", actor, now));
        return AssessmentResponse.from(risk, json);
    }
    @Transactional
    public AssessmentResponse review(UUID id, ReviewRequest request, String actor) {
        var p = payments.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
        if (!Set.of(PaymentStatus.DRAFT, PaymentStatus.ORDER_CREATED, PaymentStatus.AUTHORIZED).contains(p.getStatus()) ||
                operations.existsByPaymentIdAndState(id, "PENDING")) throw conflict("Review is locked while a payment operation is pending or the payment is closed.");
        var risk = risks.findById(id).orElseThrow(() -> conflict("Assess this payment before recording a decision."));
        String notes = request.notes().strip();
        if (notes.length() < 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a review reason of at least 10 characters.");
        if (risk.getDecision() == request.decision() && Objects.equals(risk.getReviewNotes(), notes)) return AssessmentResponse.from(risk, json);
        var now = OffsetDateTime.now(clock);
        risk.review(request.decision(), notes, actor, now);
        activities.save(new PaymentActivity(id, "REVIEW_" + request.decision().name(), actor, now, notes));
        return AssessmentResponse.from(risk, json);
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
