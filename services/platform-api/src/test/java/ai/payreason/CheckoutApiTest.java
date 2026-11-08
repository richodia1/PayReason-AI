package ai.payreason;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:checkout;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "payreason.security.username=admin", "payreason.security.password=test-password-only",
        "payreason.paypal.client-id=fixture-client", "payreason.paypal.client-secret=fixture-secret", "payreason.paypal.webhook-id=fixture-hook"})
class CheckoutApiTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final Map<String, FixtureOrder> ORDERS = new ConcurrentHashMap<>();
    static final Map<String, String> KEYS = new ConcurrentHashMap<>();
    static final AtomicInteger CAPTURES = new AtomicInteger();
    static volatile boolean loseCaptureResponse, aiUnavailable, signatureValid = true;
    static final HttpServer PROVIDER = provider();
    @LocalServerPort int port;
    HttpClient http;
    String csrf;

    static class FixtureOrder {
        String id, reference, value, currency, auth, capture;
        boolean approved, voided;
        Map<String, String> amount() { return Map.of("value", value, "currency_code", currency); }
        Map<String, Object> authorization() { return Map.of("id", auth, "status", voided ? "VOIDED" : "CREATED", "amount", amount()); }
        Map<String, Object> capture() { return Map.of("id", capture, "status", "COMPLETED", "amount", amount()); }
        Map<String, Object> order() {
            var payment = new HashMap<String, Object>();
            if (auth != null) payment.put("authorizations", List.of(authorization()));
            if (capture != null) payment.put("captures", List.of(capture()));
            return Map.of("id", id, "intent", "AUTHORIZE", "status", approved ? "APPROVED" : "CREATED",
                    "purchase_units", List.of(Map.of("reference_id", reference, "amount", amount(), "payments", payment)),
                    "links", List.of(Map.of("rel", "payer-action", "href", "https://www.sandbox.paypal.com/checkoutnow?token=" + id)));
        }
    }
    static HttpServer provider() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try { handle(exchange); }
                catch (Exception e) { respond(exchange, 500, Map.of("fixture_error", e.getClass().getSimpleName())); }
                finally { exchange.close(); }
            });
            server.start(); return server;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    static void respond(HttpExchange exchange, int code, Object body) throws java.io.IOException {
        byte[] data = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, data.length); exchange.getResponseBody().write(data);
    }
    static void handle(HttpExchange e) throws Exception {
        String path = e.getRequestURI().getPath();
        if (path.equals("/v1/oauth2/token")) { respond(e, 200, Map.of("access_token", "fixture-token")); return; }
        if (path.equals("/capabilities")) { respond(e, 200, Map.of("scoring_available", true)); return; }
        if (path.equals("/v1/scores")) {
            if (aiUnavailable) { respond(e, 503, Map.of()); return; }
            var body = JSON.readTree(e.getRequestBody().readAllBytes());
            respond(e, 200, Map.of("payment_id", body.path("payment_id").asText(), "score", 0.8, "band", "HIGH",
                    "source", "SYNTHETIC_DEMO", "model_version", "test-fixture-v1", "contributions", List.of(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()))); return;
        }
        if (path.equals("/v1/notifications/verify-webhook-signature")) {
            respond(e, 200, Map.of("verification_status", signatureValid ? "SUCCESS" : "FAILURE")); return;
        }
        assertEquals("Bearer fixture-token", e.getRequestHeaders().getFirst("Authorization"));
        String key = e.getRequestHeaders().getFirst("PayPal-Request-Id");
        if (path.equals("/v2/checkout/orders")) {
            assertNotNull(key); UUID.fromString(key);
            var body = JSON.readTree(e.getRequestBody().readAllBytes());
            assertEquals("AUTHORIZE", body.path("intent").asText());
            String orderId = KEYS.get(key);
            if (orderId == null) {
                var order = new FixtureOrder(); order.id = "ORDER" + (ORDERS.size() + 1);
                var unit = body.path("purchase_units").path(0);
                order.reference = unit.path("reference_id").asText(); order.value = unit.path("amount").path("value").asText(); order.currency = unit.path("amount").path("currency_code").asText();
                ORDERS.put(order.id, order); KEYS.put(key, order.id); orderId = order.id;
            }
            respond(e, 201, ORDERS.get(orderId).order()); return;
        }
        if (path.startsWith("/v2/checkout/orders/")) {
            var order = ORDERS.get(path.split("/")[4]);
            if (path.endsWith("/authorize")) {
                if (!order.approved) { respond(e, 422, Map.of("name", "ORDER_NOT_APPROVED")); return; }
                order.auth = "AUTH" + order.id;
            }
            respond(e, 200, order.order()); return;
        }
        if (path.startsWith("/v2/payments/authorizations/")) {
            String auth = path.split("/")[4];
            var order = ORDERS.values().stream().filter(o -> auth.equals(o.auth)).findFirst().orElseThrow();
            if (path.endsWith("/capture")) {
                assertNotNull(key);
                if (order.capture == null) { order.capture = "CAP" + order.id; CAPTURES.incrementAndGet(); KEYS.put(key, order.capture); }
                assertEquals(order.capture, KEYS.get(key));
                if (loseCaptureResponse) { loseCaptureResponse = false; respond(e, 500, Map.of()); return; }
                respond(e, 201, order.capture()); return;
            }
            if (path.endsWith("/void")) { order.voided = true; e.sendResponseHeaders(204, -1); return; }
            respond(e, 200, order.authorization()); return;
        }
        if (path.startsWith("/v2/payments/captures/")) {
            String capture = path.split("/")[4];
            respond(e, 200, ORDERS.values().stream().filter(o -> capture.equals(o.capture)).findFirst().orElseThrow().capture()); return;
        }
        respond(e, 404, Map.of());
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("payreason.paypal.base-url", () -> "http://127.0.0.1:" + PROVIDER.getAddress().getPort());
        registry.add("payreason.ai.base-url", () -> "http://127.0.0.1:" + PROVIDER.getAddress().getPort());
    }
    @AfterAll static void stopProvider() { PROVIDER.stop(0); }
    @BeforeEach void login() throws Exception {
        aiUnavailable = false; loseCaptureResponse = false; signatureValid = true;
        http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        csrf = JSON.readTree(call("GET", "/api/auth/csrf", null, null).body()).path("token").asText();
        assertEquals(204, call("POST", "/api/auth/login", "username=admin&password=test-password-only", null).statusCode());
        csrf = JSON.readTree(call("GET", "/api/auth/csrf", null, null).body()).path("token").asText();
    }
    HttpResponse<String> call(String method, String path, String body, String key) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        if (key != null) request.header("Idempotency-Key", key);
        if (body != null) request.header("Content-Type", path.endsWith("login") ? "application/x-www-form-urlencoded" : "application/json");
        return http.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    String draft() throws Exception {
        String id = UUID.randomUUID().toString();
        assertEquals(200, call("POST", "/api/payments", "{\"amount\":\"10.00\",\"currency\":\"USD\",\"channel\":\"MOBILE\"}", id).statusCode()); return id;
    }
    String path(String id, String action) { return "/api/payments/" + id + "/paypal/" + action; }
    void assess(String id) throws Exception {
        var result = call("POST", "/api/payments/" + id + "/risk", "{\"amount_ratio\":4,\"account_age_days\":10,\"transactions_last_hour\":6,\"new_device\":true,\"cross_border\":false}", null);
        assertEquals(200, result.statusCode(), result.body());
        assertEquals("HOLD", JSON.readTree(result.body()).path("decision").asText());
    }
    HttpResponse<String> review(String id, String decision) throws Exception {
        return call("POST", "/api/payments/" + id + "/risk/review", "{\"decision\":\"" + decision + "\",\"notes\":\"Reviewed the synthetic scenario.\"}", null);
    }
    void authorize(String id) throws Exception {
        var created = call("POST", path(id, "order"), null, null);
        assertEquals(200, created.statusCode(), created.body());
        String order = JSON.readTree(created.body()).path("paypalOrderId").asText();
        ORDERS.get(order).approved = true;
        var authorized = call("POST", path(id, "authorize"), null, null);
        assertEquals(200, authorized.statusCode(), authorized.body());
        assertEquals("AUTHORIZED", JSON.readTree(authorized.body()).path("status").asText());
    }

    @Test void captureRequiresReviewAndRetriesUseSameProviderKey() throws Exception {
        String id = draft(); authorize(id);
        assertEquals(409, call("POST", path(id, "capture"), null, null).statusCode());
        assess(id);
        assertEquals(409, call("POST", path(id, "capture"), null, null).statusCode());
        assertEquals(200, review(id, "RELEASE").statusCode());
        int before = CAPTURES.get(); loseCaptureResponse = true;
        assertEquals(502, call("POST", path(id, "capture"), null, null).statusCode());
        assertEquals(409, review(id, "BLOCK").statusCode());
        var retry = call("POST", path(id, "capture"), null, null);
        assertEquals(200, retry.statusCode(), retry.body());
        assertEquals("CAPTURED", JSON.readTree(retry.body()).path("status").asText());
        assertEquals(before + 1, CAPTURES.get());
        assertEquals(200, call("POST", path(id, "capture"), null, null).statusCode());
        assertEquals(before + 1, CAPTURES.get());
        assertEquals(409, review(id, "BLOCK").statusCode());
    }
    @Test void blockedPaymentsVoidInsteadOfCaptureAndCaseCountsAreFiltered() throws Exception {
        String id = draft(); assess(id); authorize(id);
        assertEquals(200, review(id, "BLOCK").statusCode());
        assertEquals(409, call("POST", path(id, "capture"), null, null).statusCode());
        var result = call("POST", path(id, "void"), null, null);
        assertEquals("VOIDED", JSON.readTree(result.body()).path("status").asText());
        assertEquals(200, call("POST", path(id, "reconcile"), null, null).statusCode());
        var cases = call("GET", "/api/cases?channel=MOBILE&decision=BLOCK", null, null);
        assertEquals(200, cases.statusCode(), cases.body()); assertTrue(JSON.readTree(cases.body()).path("total").asLong() >= 1);
        assertEquals(0, JSON.readTree(call("GET", "/api/cases?channel=WEB&decision=BLOCK", null, null).body()).path("total").asLong());
        assertEquals(0, JSON.readTree(call("GET", "/api/cases?year=2000", null, null).body()).path("total").asLong());
        assertTrue(call("GET", "/api/payments/" + id + "/activity", null, null).body().contains("Reviewed the synthetic scenario."));
    }
    @Test void unavailableAiFailsClosedAndInvalidInputRejected() throws Exception {
        String id = draft(); aiUnavailable = true;
        assertEquals(503, call("POST", "/api/payments/" + id + "/risk", "{\"amount_ratio\":1,\"account_age_days\":10,\"transactions_last_hour\":1,\"new_device\":false,\"cross_border\":false}", null).statusCode());
        assertEquals(404, call("GET", "/api/payments/" + id + "/risk", null, null).statusCode());
        assertEquals(409, review(id, "RELEASE").statusCode());
        assertEquals(400, call("POST", "/api/payments/" + id + "/risk", "{}", null).statusCode());
    }
    @Test void buyerApprovalIsRequiredAndCancelCannotRacePendingOrder() throws Exception {
        String id = draft();
        var order = call("POST", path(id, "order"), null, null);
        assertEquals(200, order.statusCode());
        assertEquals(502, call("POST", path(id, "authorize"), null, null).statusCode());
        assertEquals(409, call("POST", "/api/payments/" + id + "/cancel", null, null).statusCode());
        ORDERS.get(JSON.readTree(order.body()).path("paypalOrderId").asText()).approved = true;
        assertEquals(200, call("POST", path(id, "authorize"), null, null).statusCode());
    }
    @Test void invalidAndDuplicateWebhooksCannotSettlePayment() throws Exception {
        String id = draft(); authorize(id);
        var payment = JSON.readTree(call("GET", "/api/payments/" + id, null, null).body());
        String body = JSON.writeValueAsString(Map.of("id", "WH" + id, "event_type", "CHECKOUT.ORDER.APPROVED",
                "resource", Map.of("id", payment.path("paypalOrderId").asText())));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/paypal/webhooks"))
                .header("Content-Type", "application/json");
        for (String name : List.of("auth-algo", "cert-url", "transmission-id", "transmission-sig", "transmission-time")) request.header(("paypal-" + name).toUpperCase(Locale.ROOT), "fixture");
        var unsignedSession = HttpClient.newHttpClient();
        signatureValid = false;
        assertEquals(403, unsignedSession.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        signatureValid = true;
        assertEquals(200, unsignedSession.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        assertTrue(unsignedSession.send(request.build(), HttpResponse.BodyHandlers.ofString()).body().contains("DUPLICATE"));
        assertEquals("AUTHORIZED", JSON.readTree(call("GET", "/api/payments/" + id, null, null).body()).path("status").asText());
    }
    @Test void reportsUseRealRowsAndUnsupportedQuestionsDoNotInventAnswers() throws Exception {
        String id = draft(); assess(id); review(id, "BLOCK");
        var answer = call("POST", "/api/insights/questions", "{\"question\":\"How many blocked cases from mobile this year?\"}", null);
        assertEquals(200, answer.statusCode(), answer.body());
        assertTrue(JSON.readTree(answer.body()).path("count").asLong() >= 1);
        assertEquals("DATABASE_QUERY", JSON.readTree(answer.body()).path("method").asText());
        assertTrue(answer.body().contains(id));
        var none = call("POST", "/api/insights/questions", "{\"question\":\"How many cases in 2000?\"}", null);
        assertEquals(0, JSON.readTree(none.body()).path("count").asLong());
        assertEquals(422, call("POST", "/api/insights/questions", "{\"question\":\"How many ATM cases?\"}", null).statusCode());
    }
    @Test void assessmentIsImmutableAndCountsRejectInvalidFilters() throws Exception {
        String id = draft(); assess(id); assess(id);
        assertEquals(409, call("POST", "/api/payments/" + id + "/risk", "{\"amount_ratio\":2,\"account_age_days\":10,\"transactions_last_hour\":6,\"new_device\":true,\"cross_border\":false}", null).statusCode());
        assertEquals(400, call("GET", "/api/cases?size=1000", null, null).statusCode());
        assertEquals(400, call("GET", "/api/cases?year=100", null, null).statusCode());
        assertEquals(400, call("POST", "/api/payments/" + id + "/risk/review", "{\"decision\":\"RELEASE\",\"notes\":\"   \"}", null).statusCode());
    }
}
