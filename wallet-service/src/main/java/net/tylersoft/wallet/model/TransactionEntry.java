package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("trx_transaction_entries")
@Getter @Setter @NoArgsConstructor
public class TransactionEntry {

    @Id
    private Integer id;

    private Long esbRef;
    private Long chargeId;
    private Boolean reversed;
    private String accountNumber;
    private BigDecimal actualBalanceBefore;
    private BigDecimal availableBalanceBefore;
    private BigDecimal amount;
    private BigDecimal actualBalanceAfter;
    private BigDecimal availableBalanceAfter;
    private String currency;
    private String narration;
    private String drCr;
    private Boolean isBalanceUpdated;
    private String ledgerCode;
    private Short status;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
}
