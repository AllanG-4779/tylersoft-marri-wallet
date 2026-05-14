package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "events", value = "ticket_validations")
@Getter @Setter @NoArgsConstructor
public class TicketValidation {

    @Id
    private UUID id;

    private String ticketCode;
    private UUID eventId;
    private UUID ticketId;
    private String validatedBy;
    private String result;
    private String notes;
    private OffsetDateTime validatedAt;
}
