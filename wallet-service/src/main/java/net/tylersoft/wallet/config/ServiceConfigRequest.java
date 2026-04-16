package net.tylersoft.wallet.config;

import java.math.BigDecimal;

public record ServiceConfigRequest(
        Integer serviceId,
        String externalServiceId,
        Integer channelId,
        String serviceCode,
        boolean isExternal,
        Long accountId,
        String senderNarration,
        String receiverNarration,
        BigDecimal dailyLimit,
        BigDecimal weeklyLimit,
        BigDecimal monthlyLimit,
        String description,
        String createdBy
) {}
