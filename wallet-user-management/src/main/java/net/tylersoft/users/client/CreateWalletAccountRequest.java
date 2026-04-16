package net.tylersoft.users.client;

import java.math.BigDecimal;

/**
 * Mirrors the {@code UniversalRequestWrapper<OpenAccountRequest>} structure
 * expected by the wallet-service {@code POST /api/v2/accounts} endpoint.
 */
public record CreateWalletAccountRequest(
        AccountData data,
        ChannelDetails channelDetails
) {
    public record AccountData(
            String currency,
            String accountPrefix,
            String phoneNumber,
            String accountName,
            BigDecimal openingBalance
    ) {}

    public record ChannelDetails(String channelName) {}

    public static CreateWalletAccountRequest of(String phoneNumber,
                                                String accountName,
                                                String currency) {
        return new CreateWalletAccountRequest(
                new AccountData(currency, "WA", phoneNumber, accountName, BigDecimal.ZERO),
                new ChannelDetails("wallet-user-management")
        );
    }
}
