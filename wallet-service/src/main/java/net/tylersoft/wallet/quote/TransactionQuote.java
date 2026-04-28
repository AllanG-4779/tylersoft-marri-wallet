package net.tylersoft.wallet.quote;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("trx_quotes")
@Getter @Setter @NoArgsConstructor
class TransactionQuote {

    @Id
    private Long id;
    private String token;
    private String transactionType;
    private String transactionCode;
    private String debitAccount;
    private String creditAccount;
    private String recipientPhone;
    private String phoneNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal totalDebit;
    private String currency;
    private String recipientName;
    private String status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
