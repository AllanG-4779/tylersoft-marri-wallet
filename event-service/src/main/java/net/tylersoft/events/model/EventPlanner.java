package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "events", value = "event_planners")
@Getter @Setter @NoArgsConstructor
public class EventPlanner {

    @Id
    private UUID id;

    private UUID eventId;
    private UUID customerId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
