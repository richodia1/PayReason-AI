package ai.payreason.risk.controller;

import ai.payreason.payments.enums.PaymentChannel;
import ai.payreason.risk.enums.ReviewDecision;
import ai.payreason.risk.services.CaseQueryService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class CaseController {
    private final CaseQueryService cases;
    public CaseController(CaseQueryService cases) { this.cases = cases; }
    @GetMapping("/api/cases")
    public CaseQueryService.CasePage cases(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) PaymentChannel channel,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) ReviewDecision decision) {
        return cases.query(page, size, channel, year, decision);
    }
}
