package net.tylersoft.wallet.config;

import net.tylersoft.wallet.charge.ChargeType;
import net.tylersoft.wallet.charge.ChargeValueType;

import java.math.BigDecimal;

public record ChargeConfigRequest(
        Integer serviceManagementId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal chargeValue,
        ChargeValueType valueType,
        ChargeType chargeType,
        Long accountId,
        String senderNarration,
        String receiverNarration,
        String createdBy
) {}
