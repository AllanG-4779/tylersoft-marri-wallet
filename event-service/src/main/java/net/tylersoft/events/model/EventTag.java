package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("event_tags")
@Getter @Setter @NoArgsConstructor
public class EventTag {

    private UUID eventId;
    private UUID tagId;
}
