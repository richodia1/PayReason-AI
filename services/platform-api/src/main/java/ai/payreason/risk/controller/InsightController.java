package ai.payreason.risk.controller;

import ai.payreason.risk.services.InsightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class InsightController {
    public record Question(@NotBlank @Size(max = 300) String question) {}
    private final InsightService service;
    public InsightController(InsightService service) { this.service = service; }
    @PostMapping("/api/insights/questions")
    public Map<String, Object> answer(@RequestBody @Valid Question request) { return service.answer(request.question()); }
}
