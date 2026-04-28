package net.tylersoft.wallet.account;

import java.math.BigDecimal;

public record AccountSummary(
        String accountNumber,
        String accountName,
        String currency,
        BigDecimal actualBalance,
        BigDecimal availableBalance,
        boolean blocked,
        boolean dormant
) {}
