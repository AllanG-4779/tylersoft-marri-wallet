package net.tylersoft.wallet.admin;

import java.math.BigDecimal;
import java.util.List;

public record TransactionSummaryResponse(
        int totalCount,
        BigDecimal totalVolume,
        BigDecimal totalCharges,
        List<TypeGroup> byType,
        List<StatusGroup> byStatus
) {
    public record TypeGroup(String type, int count, BigDecimal volume) {}
    public record StatusGroup(String status, int count) {}
}
