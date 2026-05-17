package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("event_categories")
@Getter @Setter @NoArgsConstructor
public class EventCategory {

    @Id
    private UUID id;
    private UUID parentId;
    private String name;
    private String slug;
    private String iconUrl;
    private int sortOrder;
    private boolean isActive;
    private OffsetDateTime createdAt;
}
