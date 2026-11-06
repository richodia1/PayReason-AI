package ai.payreason.risk.dto;

import ai.payreason.risk.enums.ReviewDecision;
import jakarta.validation.constraints.*;

public record ReviewRequest(@NotNull ReviewDecision decision, @NotBlank @Size(min = 10, max = 2000) String notes) {}
