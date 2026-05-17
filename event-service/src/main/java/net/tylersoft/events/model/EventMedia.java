package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("event_media")
@Getter @Setter @NoArgsConstructor
public class EventMedia {

    @Id
    private UUID id;
    private UUID eventId;
    private String mediaType;
    private String url;
    private String caption;
    private int sortOrder;
    private OffsetDateTime createdAt;
}
