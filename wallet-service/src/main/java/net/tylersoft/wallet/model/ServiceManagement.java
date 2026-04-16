package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("cfg_service_management")
@Getter @Setter @NoArgsConstructor
public class ServiceManagement {

    @Id
    private Integer id;
    private Integer serviceId;
    private String externalServiceId;
    private Integer ledgerAccountId;
    private Long accountId;
    private Integer channelId;
    private Integer requestDirectionId;
    private String serviceCode;
    private String receiverNarration;
    private String senderNarration;
    private BigDecimal lastHour;
    private BigDecimal dailyLimit;
    private BigDecimal weeklyLimit;
    private BigDecimal monthlyLimit;
    private String description;
    private Short status;
    @Column("is_external")
    private boolean isExternal;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
}
