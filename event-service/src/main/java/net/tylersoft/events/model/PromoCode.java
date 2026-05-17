package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tylersoft.events.common.DiscountType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("promo_codes")
@Getter @Setter @NoArgsConstructor
public class PromoCode {

    @Id
    private UUID id;
    private UUID eventId;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer maxUses;
    private int usesCount;
    private BigDecimal minOrderAmount;
    private OffsetDateTime startsAt;
    private OffsetDateTime expiresAt;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
