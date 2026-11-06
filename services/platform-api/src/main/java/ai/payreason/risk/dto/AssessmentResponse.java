package ai.payreason.risk.dto;

import ai.payreason.risk.entities.RiskAssessment;
import ai.payreason.risk.enums.ReviewDecision;
import java.time.OffsetDateTime;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public record AssessmentResponse(UUID paymentId, double score, String band, String modelVersion, String source,
        JsonNode evidence, String assessedBy, OffsetDateTime assessedAt, ReviewDecision decision,
        String reviewNotes, String reviewedBy, OffsetDateTime reviewedAt) {
    public static AssessmentResponse from(RiskAssessment r, ObjectMapper json) {
        return new AssessmentResponse(r.getPaymentId(), r.getScore(), r.getBand(), r.getModelVersion(), r.getSource(),
                json.readTree(r.getEvidence()), r.getAssessedBy(), r.getAssessedAt(), r.getDecision(),
                r.getReviewNotes(), r.getReviewedBy(), r.getReviewedAt());
    }
}
