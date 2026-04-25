package net.tylersoft.wallet.account;

import java.util.List;

public record EnquiryResponse(
        String               transactionType,
        String               transactionCode,
        BalanceResponse      balance,   // populated for BI_WALLET
        List<MiniStatementEntry> entries  // populated for MINI_STATEMENT / FULL_STATEMENT
) {}
