package net.tylersoft.wallet.ott;

import java.math.BigDecimal;

public record OttVoucherGatewayRequest(
        String     reference,
        String     mobileForSms,
        BigDecimal amount,
        String     currency
) {}
