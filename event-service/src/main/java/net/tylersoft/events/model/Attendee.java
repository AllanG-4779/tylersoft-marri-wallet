package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("attendees")
@Getter @Setter @NoArgsConstructor
public class Attendee {

    @Id
    private UUID id;
    private UUID orderId;
    private UUID orderItemId;
    private UUID ticketTypeId;
    private UUID eventId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String ticketNumber;
    private String qrCodeData;
    private String barcode;
    private OffsetDateTime checkedInAt;
    private UUID checkedInBy;
    private OffsetDateTime checkedOutAt;
    private int checkInCount;
    private boolean isCancelled;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
