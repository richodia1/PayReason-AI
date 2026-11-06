package ai.payreason.risk.controller;

import ai.payreason.risk.dto.*;
import ai.payreason.risk.services.RiskService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/{id}/risk")
public class RiskController {
    private final RiskService service;
    public RiskController(RiskService service) { this.service = service; }
    @GetMapping public AssessmentResponse get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping public AssessmentResponse assess(@PathVariable UUID id, @Valid @RequestBody RiskFeatures features, Principal actor) {
        return service.assess(id, features, actor.getName());
    }
    @PostMapping("/review") public AssessmentResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request, Principal actor) {
        return service.review(id, request, actor.getName());
    }
}
