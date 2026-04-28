package net.tylersoft.wallet.account;

public record AccountEnquiryRequest(
        String accountNumber,
        String transactionType,  // "BI" or "STATEMENT"
        String transactionCode,  // "BI_WALLET" | "MINI_STATEMENT" | "FULL_STATEMENT"
        Integer limit,           // used by MINI_STATEMENT (default 10, omit for other types)
        String fromDate,         // used by FULL_STATEMENT (yyyy-MM-dd)
        String toDate            // used by FULL_STATEMENT (yyyy-MM-dd)
) {}
