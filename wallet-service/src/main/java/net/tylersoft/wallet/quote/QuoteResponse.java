package net.tylersoft.wallet.quote;

import java.math.BigDecimal;

public record QuoteResponse(
        String quoteToken,
        String transactionType,
        String transactionCode,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal totalDebit,
        String currency,
        String recipientName,    // resolved account name for FT
        String recipientAccount, // credit account number for FT
        String recipientPhone,   // phone number for AIRTIME
        String expiresAt,
        String summary
) {}
