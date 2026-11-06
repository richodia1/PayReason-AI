package ai.payreason.audit.dto;

import ai.payreason.audit.entities.PaymentActivity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivityResponse(UUID id, String action, String actor, OffsetDateTime occurredAt, String notes) {
    public static ActivityResponse from(PaymentActivity activity) {
        return new ActivityResponse(activity.getId(), activity.getAction(), activity.getActor(), activity.getOccurredAt(), activity.getNotes());
    }
}
