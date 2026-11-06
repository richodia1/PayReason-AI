package ai.payreason.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RiskFeatures(
        @JsonProperty("amount_ratio") @NotNull @DecimalMin("0.01") @DecimalMax("30") BigDecimal amountRatio,
        @JsonProperty("account_age_days") @NotNull @Min(0) @Max(3650) Integer accountAgeDays,
        @JsonProperty("transactions_last_hour") @NotNull @Min(0) @Max(50) Integer transactionsLastHour,
        @JsonProperty("new_device") @NotNull Boolean newDevice,
        @JsonProperty("cross_border") @NotNull Boolean crossBorder) {}
