package net.tylersoft.wallet.account;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MiniStatementEntry(
        String reference,
        String transactionType,
        String transactionCode,
        String drCr,
        BigDecimal amount,
        String currency,
        String narration,
        String status,
        String receiptNumber,
        OffsetDateTime date
) {}
