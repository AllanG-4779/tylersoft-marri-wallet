package net.tylersoft.events.dto.planner;

import net.tylersoft.events.model.EventPlanner;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlannerResponse(
        UUID id,
        UUID eventId,
        UUID customerId,
        String name,
        String email,
        String phone,
        String role,
        String status,
        OffsetDateTime createdAt
) {
    public static PlannerResponse from(EventPlanner p) {
        return new PlannerResponse(
                p.getId(),
                p.getEventId(),
                p.getCustomerId(),
                p.getName(),
                p.getEmail(),
                p.getPhone(),
                p.getRole(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }
}
