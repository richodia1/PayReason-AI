package ai.payreason.payments.dto;

import ai.payreason.payments.entities.PaymentDraft;
import ai.payreason.payments.enums.PaymentChannel;
import ai.payreason.payments.enums.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(UUID id, String reference, String customerReference, String amount,
                              String currency, PaymentChannel channel, PaymentStatus status,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt, String paypalOrderId,
                              String authorizationId, String captureId, String approvalUrl) {
    public static PaymentResponse from(PaymentDraft draft) {
        return new PaymentResponse(draft.getId(), draft.getReference(), draft.getCustomerReference(),
                draft.getAmount().setScale(2).toPlainString(), draft.getCurrency(), draft.getChannel(),
                draft.getStatus(), draft.getCreatedAt(), draft.getUpdatedAt(), draft.getPaypalOrderId(),
                draft.getAuthorizationId(), draft.getCaptureId(), draft.getApprovalUrl());
    }
}
