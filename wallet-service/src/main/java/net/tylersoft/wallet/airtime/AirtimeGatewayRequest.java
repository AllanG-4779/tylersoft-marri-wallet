package net.tylersoft.wallet.airtime;

import java.math.BigDecimal;

public record AirtimeGatewayRequest(
        String     reference,
        String     network,
        String     recipientPhone,
        BigDecimal amount,
        String     currency
) {}
