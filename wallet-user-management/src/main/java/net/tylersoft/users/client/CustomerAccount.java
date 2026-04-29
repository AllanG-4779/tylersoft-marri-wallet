package net.tylersoft.users.client;

import java.math.BigDecimal;

public record CustomerAccount(
        String accountNumber,
        String accountName,
        String currency,
        BigDecimal actualBalance,
        BigDecimal availableBalance,
        boolean blocked,
        boolean dormant
) {}
