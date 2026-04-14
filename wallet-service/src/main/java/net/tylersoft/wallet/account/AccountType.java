package net.tylersoft.wallet.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("acc_account_types")
@Getter @Setter @NoArgsConstructor
public class AccountType {

    @Id
    private Integer id;

    private String accountPrefix;
    private Integer accountNumberLength;
    private Integer minAccounts;
    private Integer maxAccounts;
    private String typeName;
    private BigDecimal yearlyLimit;
    private BigDecimal minBalanceLimit;
    private BigDecimal maxBalanceLimit;
    private Boolean canOverdraw;
    private BigDecimal overdrawLimit;
    private AccountCategory category;
    private String description;
    private Short status;
    private String createdBy;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private String updateBy;
    private LocalDateTime deletedOn;
    private String deletedBy;
    private Boolean accountPanEnabled;
}
