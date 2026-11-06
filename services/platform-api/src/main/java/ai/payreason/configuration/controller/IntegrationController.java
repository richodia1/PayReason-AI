package ai.payreason.configuration.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IntegrationController {
    private final boolean paypalCredentialsPresent;
    private final ai.payreason.risk.services.ScoringClient scorer;

    public IntegrationController(@Value("${payreason.paypal.client-id}") String id,
                                 @Value("${payreason.paypal.client-secret}") String secret,
                                 ai.payreason.risk.services.ScoringClient scorer) {
        paypalCredentialsPresent = !id.isBlank() && !secret.isBlank();
        this.scorer = scorer;
    }

    @GetMapping("/api/integrations")
    public Map<String, Object> integrations() {
        boolean available = false;
        try { available = scorer.capabilities().path("scoring_available").asBoolean(); }
        catch (org.springframework.web.server.ResponseStatusException ignored) { }
        return Map.of("environment", "SANDBOX", "phase", "DEMO", "integrations", List.of(
                Map.of("id", "paypal", "name", "PayPal Sandbox", "status", paypalCredentialsPresent ? "CONFIGURED" : "NOT_CONNECTED",
                        "credentialsPresent", paypalCredentialsPresent, "detail", "Authorize, review, then capture or void. Credentials are not a connectivity check."),
                Map.of("id", "ml", "name", "Synthetic risk model", "status", available ? "AVAILABLE" : "UNAVAILABLE",
                        "detail", "Logistic regression with exact feature contributions. Synthetic training data only."),
                Map.of("id", "assistant", "name", "Case reporting", "status", "AVAILABLE",
                        "detail", "Database-backed counts and evidence. No generative model or RAG connected.")));
    }
}
