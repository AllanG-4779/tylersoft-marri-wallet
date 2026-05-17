package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("tags")
@Getter @Setter @NoArgsConstructor
public class Tag {

    @Id
    private UUID id;
    private String name;
    private String slug;
    private OffsetDateTime createdAt;
}
