package ai.payreason.risk.services;

import ai.payreason.payments.entities.PaymentDraft;
import ai.payreason.risk.dto.RiskFeatures;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ScoringClient {
    private final String baseUrl;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public ScoringClient(@Value("${payreason.ai.base-url}") String baseUrl, ObjectMapper json) {
        this.baseUrl = baseUrl; this.json = json;
    }
    public JsonNode capabilities() { return call("GET", "/capabilities", null); }
    public JsonNode score(PaymentDraft p, RiskFeatures features) {
        var result = call("POST", "/v1/scores", Map.of("payment_id", p.getId().toString(), "amount", p.getAmount(),
                "currency", p.getCurrency(), "channel", p.getChannel(), "features", features));
        double score = result.path("score").asDouble(Double.NaN);
        if (!Double.isFinite(score) || score < 0 || score > 1 || !result.path("payment_id").asText().equals(p.getId().toString())
                || !java.util.Set.of("LOW", "MEDIUM", "HIGH").contains(result.path("band").asText())
                || !result.path("source").asText().equals("SYNTHETIC_DEMO") || result.path("model_version").asText().isBlank()
                || !result.path("contributions").isArray() || result.path("contributions").size() != 5) throw unavailable();
        return result;
    }
    private JsonNode call(String method, String path, Object body) {
        try {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw unavailable();
            return json.readTree(response.body());
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw unavailable(); }
        catch (Exception exception) { throw unavailable(); }
    }
    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Risk model unavailable. Payment remains held; retry when the AI service is healthy.");
    }
}
