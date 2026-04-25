package net.tylersoft.wallet.account;

import java.math.BigDecimal;

public record BalanceResponse(
        String accountNumber,
        String accountName,
        String currency,
        BigDecimal actualBalance,
        BigDecimal availableBalance
) {}
