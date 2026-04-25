package net.tylersoft.wallet.config;

import java.math.BigDecimal;

public record ServiceConfigRequest(
        String externalServiceId,
        String serviceCode,
        boolean isExternal,
        /** ISO currency code for the auto-created GL account (e.g. "BWP"). Only used when isExternal=true. */
        String currency,
        String senderNarration,
        String receiverNarration,
        String transactionType,
        BigDecimal dailyLimit,
        BigDecimal weeklyLimit,
        BigDecimal monthlyLimit,
        String description,
        String createdBy
) {}
