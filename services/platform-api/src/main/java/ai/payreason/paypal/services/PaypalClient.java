package ai.payreason.paypal.services;

import ai.payreason.payments.entities.PaymentDraft;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaypalClient {
    private final String baseUrl, clientId, secret, returnUrl, webhookId;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    public static class ProviderFailure extends ResponseStatusException {
        public final boolean uncertain;
        public ProviderFailure(String message, boolean uncertain) {
            super(HttpStatus.BAD_GATEWAY, message); this.uncertain = uncertain;
        }
    }
    public PaypalClient(@Value("${payreason.paypal.base-url}") String baseUrl,
            @Value("${payreason.paypal.client-id}") String clientId,
            @Value("${payreason.paypal.client-secret}") String secret,
            @Value("${payreason.paypal.return-url}") String returnUrl,
            @Value("${payreason.paypal.webhook-id}") String webhookId, ObjectMapper json) {
        URI uri = URI.create(baseUrl);
        if (!baseUrl.equals("https://api-m.sandbox.paypal.com") &&
                !("http".equals(uri.getScheme()) && Set.of("localhost", "127.0.0.1").contains(uri.getHost()))) {
            throw new IllegalArgumentException("This edition supports PayPal Sandbox only.");
        }
        this.baseUrl = baseUrl; this.clientId = clientId; this.secret = secret; this.returnUrl = returnUrl;
        this.webhookId = webhookId; this.json = json;
    }
    public boolean configured() { return !clientId.isBlank() && !secret.isBlank(); }
    public void requireConfigured() {
        if (!configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Set PayPal Sandbox credentials in the server environment. No payment was submitted.");
    }
    public JsonNode create(PaymentDraft p, UUID key) {
        return call("POST", "/v2/checkout/orders", Map.of("intent", "AUTHORIZE",
                "purchase_units", List.of(Map.of("reference_id", p.getId().toString(), "custom_id", p.getId().toString(),
                        "amount", Map.of("currency_code", p.getCurrency(), "value", p.getAmount().setScale(2).toPlainString()))),
                "payment_source", Map.of("paypal", Map.of("experience_context", Map.of(
                        "shipping_preference", "NO_SHIPPING", "user_action", "CONTINUE",
                        "return_url", returnUrl, "cancel_url", returnUrl)))), key);
    }
    public JsonNode authorize(String orderId, UUID key) { return call("POST", "/v2/checkout/orders/" + id(orderId) + "/authorize", Map.of(), key); }
    public JsonNode capture(String authId, UUID key) { return call("POST", "/v2/payments/authorizations/" + id(authId) + "/capture", Map.of("final_capture", true), key); }
    public void voidAuthorization(String authId, UUID key) { call("POST", "/v2/payments/authorizations/" + id(authId) + "/void", null, key); }
    public JsonNode order(String orderId) { return call("GET", "/v2/checkout/orders/" + id(orderId), null, null); }
    public JsonNode authorization(String authId) { return call("GET", "/v2/payments/authorizations/" + id(authId), null, null); }
    public JsonNode captured(String captureId) { return call("GET", "/v2/payments/captures/" + id(captureId), null, null); }
    public boolean verify(JsonNode event, Map<String, String> headers) {
        if (webhookId.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PayPal webhook verification is not configured.");
        var body = new HashMap<String, Object>();
        var normalizedHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        normalizedHeaders.putAll(headers);
        for (String field : List.of("auth_algo", "cert_url", "transmission_id", "transmission_sig", "transmission_time")) {
            String value = normalizedHeaders.get("paypal-" + field.replace('_', '-'));
            if (value == null || value.isBlank() || value.length() > 4000)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid PayPal signature headers.");
            body.put(field, value);
        }
        body.put("webhook_id", webhookId); body.put("webhook_event", event);
        return "SUCCESS".equals(call("POST", "/v1/notifications/verify-webhook-signature", body, null).path("verification_status").asText());
    }
    public static String id(String value) {
        if (value == null || !value.matches("[A-Za-z0-9]{1,64}")) throw new ProviderFailure("PayPal returned an invalid resource identifier.", true);
        return value;
    }
    public static String approval(JsonNode order) {
        for (JsonNode link : order.path("links")) {
            if (Set.of("approve", "payer-action").contains(link.path("rel").asText())) {
                URI uri = URI.create(link.path("href").asText());
                if ("https".equals(uri.getScheme()) && Set.of("www.sandbox.paypal.com", "sandbox.paypal.com").contains(uri.getHost()) && uri.getUserInfo() == null)
                    return uri.toString();
            }
        }
        throw new ProviderFailure("PayPal did not return a Sandbox approval URL.", true);
    }
    private JsonNode call(String method, String path, Object body, UUID key) {
        requireConfigured();
        String token = token();
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .header("Prefer", "return=representation");
        if (key != null) builder.header("PayPal-Request-Id", key.toString());
        return send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build());
    }
    private String token() {
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/oauth2/token")).timeout(Duration.ofSeconds(8))
                .header("Authorization", "Basic " + basic).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials")).build();
        var token = send(request).path("access_token").asText();
        if (token.isBlank()) throw new ProviderFailure("PayPal authentication failed. Check Sandbox credentials.", false);
        return token;
    }
    private JsonNode send(HttpRequest request) {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                boolean uncertain = response.statusCode() >= 500 || response.statusCode() == 429 || response.statusCode() == 408;
                throw new ProviderFailure("PayPal returned HTTP " + response.statusCode() + ". Check buyer approval or reconcile the payment before retrying.", uncertain);
            }
            return response.body().isBlank() ? json.createObjectNode() : json.readTree(response.body());
        } catch (ProviderFailure exception) { throw exception; }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new ProviderFailure("PayPal request interrupted; reconcile before retrying.", true); }
        catch (Exception exception) { throw new ProviderFailure("PayPal response unavailable; reconcile or retry the same operation.", true); }
    }
}
