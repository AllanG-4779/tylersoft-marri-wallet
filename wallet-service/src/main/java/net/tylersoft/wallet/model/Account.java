package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("acc_accounts")
@Getter @Setter @NoArgsConstructor
public class Account {

    @Id
    private Long id;

    private String accountNumber;
    private String phoneNumber;
    private OffsetDateTime openingDate;
    private BigDecimal openingBalance;
    private BigDecimal actualBalance;
    private BigDecimal availableBalance;
    private Integer accountTypeId;
    private Integer currencyId;
    private String accountName;
    private Boolean allowDr;
    private Boolean allowCr;
    private Boolean blocked;
    private Boolean dormant;
    private Short status;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
    private String accountPan;
}
