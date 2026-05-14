package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "events", value = "events")
@Getter @Setter @NoArgsConstructor
public class Event {

    @Id
    private UUID id;

    private String merchantCode;
    private String title;
    private String description;
    private String venueName;
    private String venueAddress;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String coverImageUrl;
    private String status;
    private String statusReason;
    private OffsetDateTime statusChangedAt;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
