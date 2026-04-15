package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tylersoft.wallet.charge.ChargeType;
import net.tylersoft.wallet.charge.ChargeValueType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("transaction_charges_config")
@Getter @Setter @NoArgsConstructor
public class ChargeConfig {
    @Id
    private Integer id;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal chargeValue;
    private ChargeValueType valueType;
    private ChargeType chargeType;
    private String taxAccount;
    private Long accountId;
    private Integer ledgerAccountId;
    private Integer serviceManagementId;
    private String receiverNarration;
    private String senderNarration;
    private Short status;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
}
