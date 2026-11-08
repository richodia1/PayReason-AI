package ai.payreason.payments.dto;

import ai.payreason.payments.enums.PaymentChannel;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        @Size(max = 80) String customerReference,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "USD|GBP|EUR") String currency,
        @NotNull PaymentChannel channel) {
}
