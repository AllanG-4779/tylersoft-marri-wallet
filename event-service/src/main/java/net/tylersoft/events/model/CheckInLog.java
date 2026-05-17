package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tylersoft.events.common.CheckinStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("check_in_logs")
@Getter @Setter @NoArgsConstructor
public class CheckInLog {

    @Id
    private UUID id;
    private UUID attendeeId;
    private UUID eventId;
    private UUID staffId;
    private CheckinStatus status;
    private String deviceInfo;
    private String locationInfo;
    private String notes;
    private OffsetDateTime createdAt;
}
