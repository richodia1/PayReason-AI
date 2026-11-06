package ai.payreason.payments.controller;

import ai.payreason.audit.dto.ActivityResponse;
import ai.payreason.payments.dto.CreatePaymentRequest;
import ai.payreason.payments.dto.PaymentResponse;
import ai.payreason.payments.enums.PaymentStatus;
import ai.payreason.payments.services.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    public record PaymentPage(List<PaymentResponse> items, long total, int page, int size) {}

    @GetMapping
    public PaymentPage list(@RequestParam(defaultValue = "0") @Min(0) int page,
                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                            @RequestParam(required = false) PaymentStatus status) {
        var result = service.list(page, size, status);
        return new PaymentPage(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() { return service.summary(); }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public PaymentResponse create(@RequestHeader("Idempotency-Key") UUID requestId,
                                  @RequestBody @Valid CreatePaymentRequest request, Principal principal) {
        return service.create(requestId, request, principal.getName());
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable UUID id, Principal principal) {
        return service.cancel(id, principal.getName());
    }

    @GetMapping("/{id}/activity")
    public List<ActivityResponse> activity(@PathVariable UUID id) { return service.activity(id); }
}
