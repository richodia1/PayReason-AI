package ai.payreason.paypal.controller;

import ai.payreason.payments.dto.PaymentResponse;
import ai.payreason.paypal.enums.OperationKind;
import ai.payreason.paypal.services.CheckoutService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/{id}/paypal")
public class CheckoutController {
    private final CheckoutService checkout;
    public CheckoutController(CheckoutService checkout) { this.checkout = checkout; }
    @GetMapping("/operations")
    public java.util.List<CheckoutService.OperationView> operations(@PathVariable UUID id) { return checkout.operations(id); }
    @PostMapping("/order")
    public PaymentResponse create(@PathVariable UUID id, Principal actor) { return checkout.run(id, OperationKind.CREATE, actor.getName()); }
    @PostMapping("/authorize")
    public PaymentResponse authorize(@PathVariable UUID id, Principal actor) { return checkout.run(id, OperationKind.AUTHORIZE, actor.getName()); }
    @PostMapping("/capture")
    public PaymentResponse capture(@PathVariable UUID id, Principal actor) { return checkout.run(id, OperationKind.CAPTURE, actor.getName()); }
    @PostMapping("/void")
    public PaymentResponse cancel(@PathVariable UUID id, Principal actor) { return checkout.run(id, OperationKind.VOID, actor.getName()); }
    @PostMapping("/reconcile")
    public PaymentResponse reconcile(@PathVariable UUID id, Principal actor) { return checkout.reconcile(id, actor.getName()); }
}
