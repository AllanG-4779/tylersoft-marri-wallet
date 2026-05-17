package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tylersoft.events.common.EventStatus;
import net.tylersoft.events.common.EventTimeDisplay;
import net.tylersoft.events.common.EventType;
import net.tylersoft.events.common.EventVisibility;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("events")
@Getter @Setter @NoArgsConstructor
public class Event {

    @Id
    private UUID id;
    private UUID organizationId;
    private UUID createdBy;
    private UUID approvedBy;

    private String title;
    private String slug;
    private UUID categoryId;
    private EventType eventType;
    private EventVisibility visibility;
    private String shortDescription;
    private String description;

    private EventStatus status;
    private OffsetDateTime approvedAt;
    private String approvalNotes;
    private String rejectionReason;

    private String venueName;
    private String venueAddress;
    private String venueCity;
    private String venueCountry;
    private BigDecimal venueLatitude;
    private BigDecimal venueLongitude;
    private String onlineEventUrl;

    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private String timezone;
    private EventTimeDisplay timeDisplay;
    private Integer totalCapacity;

    private String bannerUrl;
    private String logoUrl;

    private OffsetDateTime salesStartAt;
    private OffsetDateTime salesEndAt;
    private boolean closeSalesAtCapacity;
    private int minTicketsPerOrder;
    private Integer maxTicketsPerOrder;
    private boolean allowGroupPurchases;
    private boolean showRemainingTickets;
    private boolean allowMultipleEntries;
    private boolean enableCheckinsStaff;
    private Integer minAge;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
