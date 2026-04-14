package net.tylersoft.wallet.account;

import java.math.BigDecimal;

public record OpenAccountRequest(
        String currency,
        String accountPrefix,
        String phoneNumber,      // required for THIRD_PARTY; ignored for WALLET_HOLDER (token-sourced)
        String accountName,
        BigDecimal openingBalance
) {}
