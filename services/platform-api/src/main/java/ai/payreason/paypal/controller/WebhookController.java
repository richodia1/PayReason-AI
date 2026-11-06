package ai.payreason.paypal.controller;

import ai.payreason.payments.repository.PaymentRepository;
import ai.payreason.paypal.entities.WebhookReceipt;
import ai.payreason.paypal.repository.WebhookRepository;
import ai.payreason.paypal.services.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestController
public class WebhookController {
    private final PaypalClient paypal;
    private final CheckoutService checkout;
    private final PaymentRepository payments;
    private final WebhookRepository receipts;
    private final ObjectMapper json;
    private final Clock clock;
    public WebhookController(PaypalClient paypal, CheckoutService checkout, PaymentRepository payments,
            WebhookRepository receipts, ObjectMapper json, Clock clock) {
        this.paypal = paypal; this.checkout = checkout; this.payments = payments; this.receipts = receipts; this.json = json; this.clock = clock;
    }
    @PostMapping("/api/paypal/webhooks")
    public Map<String, String> receive(@RequestHeader Map<String, String> headers, HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readNBytes(65537);
        if (bytes.length > 65536) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        var event = json.readTree(bytes);
        if (!paypal.verify(event, headers)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid webhook signature.");
        String id = event.path("id").asText(), type = event.path("event_type").asText();
        if (id.isBlank() || id.length() > 100 || type.isBlank() || type.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event identity.");
        if (receipts.existsById(id)) return Map.of("status", "DUPLICATE");
        String order = event.path("resource").path("supplementary_data").path("related_ids").path("order_id").asText();
        if (type.startsWith("CHECKOUT.ORDER.")) order = event.path("resource").path("id").asText();
        // Never trust event ordering or event amounts to settle a payment. Fetch the provider's current record.
        if (!order.isBlank()) payments.findByPaypalOrderId(order).ifPresent(p -> checkout.reconcile(p.getId(), "paypal-webhook"));
        receipts.save(new WebhookReceipt(id, type, OffsetDateTime.now(clock)));
        return Map.of("status", "RECEIVED");
    }
}
