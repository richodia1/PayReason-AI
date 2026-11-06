package ai.payreason.risk.entities;

import ai.payreason.risk.enums.ReviewDecision;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {
    @Id private UUID paymentId;
    @Column(nullable = false) private double score;
    @Column(nullable = false, length = 12) private String band;
    @Column(nullable = false, length = 100) private String modelVersion;
    @Column(nullable = false, length = 32) private String source;
    @Column(nullable = false, columnDefinition = "TEXT") private String evidence;
    @Column(nullable = false, columnDefinition = "TEXT") private String inputFeatures;
    @Column(nullable = false, length = 100) private String assessedBy;
    @Column(nullable = false) private OffsetDateTime assessedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private ReviewDecision decision;
    @Column(length = 2000) private String reviewNotes;
    @Column(length = 100) private String reviewedBy;
    private OffsetDateTime reviewedAt;
    @Version private long version;

    protected RiskAssessment() {}
    public RiskAssessment(UUID id, JsonNode result, String features, String actor, OffsetDateTime now) {
        paymentId = id; score = result.path("score").asDouble(); band = result.path("band").asText();
        modelVersion = result.path("model_version").asText(); source = result.path("source").asText();
        evidence = result.toString(); inputFeatures = features; assessedBy = actor; assessedAt = now;
        decision = ReviewDecision.HOLD;
    }
    public void review(ReviewDecision decision, String notes, String actor, OffsetDateTime now) {
        this.decision = decision; reviewNotes = notes; reviewedBy = actor; reviewedAt = now;
    }
    public UUID getPaymentId() { return paymentId; }
    public double getScore() { return score; }
    public String getBand() { return band; }
    public String getModelVersion() { return modelVersion; }
    public String getSource() { return source; }
    public String getEvidence() { return evidence; }
    public String getInputFeatures() { return inputFeatures; }
    public String getAssessedBy() { return assessedBy; }
    public OffsetDateTime getAssessedAt() { return assessedAt; }
    public ReviewDecision getDecision() { return decision; }
    public String getReviewNotes() { return reviewNotes; }
    public String getReviewedBy() { return reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
}
