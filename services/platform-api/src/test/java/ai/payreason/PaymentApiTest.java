package ai.payreason;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:payreason;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "payreason.security.username=admin", "payreason.security.password=test-password-only",
        "payreason.paypal.client-id=", "payreason.paypal.client-secret=", "payreason.paypal.webhook-id="})
class PaymentApiTest {
    @LocalServerPort int port;
    HttpClient client;
    final ObjectMapper json = new ObjectMapper();
    String token;

    @BeforeEach
    void init() { client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build(); }

    HttpResponse<String> request(String method, String path, String body, String id, boolean csrf) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (csrf) builder.header("X-CSRF-TOKEN", token);
        if (id != null) builder.header("Idempotency-Key", id);
        if (body != null) builder.header("Content-Type", path.endsWith("login")
                ? "application/x-www-form-urlencoded" : "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    void login() throws Exception {
        token = json.readTree(request("GET", "/api/auth/csrf", null, null, false).body()).get("token").asText();
        assertEquals(204, request("POST", "/api/auth/login", "username=admin&password="
                + URLEncoder.encode("test-password-only", StandardCharsets.UTF_8), null, true).statusCode());
        token = json.readTree(request("GET", "/api/auth/csrf", null, null, false).body()).get("token").asText();
    }

    String draft(String amount) {
        return "{\"amount\":\"" + amount
                + "\",\"currency\":\"GBP\",\"channel\":\"WEB\"}";
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        assertEquals(401, request("GET", "/api/payments", null, null, false).statusCode());
        login();
        assertEquals(403, request("POST", "/api/payments", draft("10.00"), UUID.randomUUID().toString(), false).statusCode());
    }

    @Test
    void createsIdempotentlyCancelsAndRecordsActor() throws Exception {
        login();
        String id = UUID.randomUUID().toString();
        var created = request("POST", "/api/payments", draft("12.34"), id, true);
        assertEquals(200, created.statusCode(), created.body());
        JsonNode payment = json.readTree(created.body());
        assertEquals("12.34", payment.get("amount").asText());
        assertEquals("DRAFT", payment.get("status").asText());
        assertEquals(id, payment.get("id").asText());
        String reference = payment.get("reference").asText();
        assertTrue(reference.matches("PR-[0-9A-F]{32}"));
        var retry = request("POST", "/api/payments", draft("12.34"), id, true);
        assertEquals(200, retry.statusCode());
        assertEquals(reference, json.readTree(retry.body()).get("reference").asText());
        assertEquals(reference, json.readTree(request("GET", "/api/payments/" + id, null, null, false).body()).get("reference").asText());
        assertEquals(409, request("POST", "/api/payments", draft("20.00"), id, true).statusCode());
        var activity = json.readTree(request("GET", "/api/payments/" + id + "/activity", null, null, false).body());
        assertEquals(1, activity.size());
        assertEquals("admin", activity.get(0).get("actor").asText());
        var cancelled = request("POST", "/api/payments/" + id + "/cancel", null, null, true);
        assertEquals("CANCELLED", json.readTree(cancelled.body()).get("status").asText());
        assertEquals(200, request("POST", "/api/payments/" + id + "/cancel", null, null, true).statusCode());
        assertEquals(2, json.readTree(request("GET", "/api/payments/" + id + "/activity", null, null, false).body()).size());
    }

    @Test
    void generatesDifferentReferencesForNewPaymentsAndRejectsManualReferences() throws Exception {
        login();
        var first = request("POST", "/api/payments", draft("15.00"), UUID.randomUUID().toString(), true);
        var second = request("POST", "/api/payments", draft("15.00"), UUID.randomUUID().toString(), true);
        assertEquals(200, first.statusCode());
        assertEquals(200, second.statusCode());
        assertNotEquals(json.readTree(first.body()).get("reference").asText(), json.readTree(second.body()).get("reference").asText());
        var manual = json.createObjectNode().put("reference", "MANUAL-REFERENCE")
                .put("amount", "15.00").put("currency", "GBP").put("channel", "WEB");
        assertEquals(400, request("POST", "/api/payments", json.writeValueAsString(manual), UUID.randomUUID().toString(), true).statusCode());
        var schema = json.readTree(request("GET", "/v3/api-docs", null, null, false).body())
                .path("components").path("schemas").path("CreatePaymentRequest").path("properties");
        assertTrue(schema.has("amount"));
        assertFalse(schema.has("reference"));
    }

    @Test
    void rejectsInvalidMoneyAndDoesNotExposeCredentials() throws Exception {
        login();
        for (String amount : new String[]{"0", "-1", "2.345", "1000000000000"}) {
            assertEquals(400, request("POST", "/api/payments", draft(amount), UUID.randomUUID().toString(), true).statusCode());
        }
        assertEquals(400, request("GET", "/api/payments?size=1001", null, null, false).statusCode());
        assertEquals(404, request("GET", "/api/payments/" + UUID.randomUUID(), null, null, false).statusCode());
        String integrations = request("GET", "/api/integrations", null, null, false).body();
        assertTrue(integrations.contains("NOT_CONNECTED"));
        assertFalse(integrations.contains("test-password-only"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        login();
        assertEquals(200, request("GET", "/api/auth/me", null, null, false).statusCode());
        assertEquals(204, request("POST", "/api/auth/logout", null, null, true).statusCode());
        assertEquals(401, request("GET", "/api/auth/me", null, null, false).statusCode());
    }
}
