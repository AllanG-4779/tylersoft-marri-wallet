package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tylersoft.events.common.TicketTierStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("pricing_tiers")
@Getter @Setter @NoArgsConstructor
public class PricingTier {

    @Id
    private UUID id;
    private UUID ticketTypeId;
    private String name;
    private BigDecimal price;
    private int quantity;
    private int quantitySold;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private TicketTierStatus status;
    private int sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
