package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("event_analytics_snapshots")
@Getter @Setter @NoArgsConstructor
public class EventAnalyticsSnapshot {

    @Id
    private UUID id;
    private UUID eventId;
    private LocalDate snapshotDate;
    private int ticketsSold;
    private BigDecimal totalRevenue;
    private int totalCheckins;
    private int totalOrders;
    private BigDecimal conversionRate;
    private BigDecimal avgTicketValue;
    private String breakdownByTicketType;
    private OffsetDateTime createdAt;
}
