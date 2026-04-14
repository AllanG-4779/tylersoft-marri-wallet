package net.tylersoft.wallet.common;

/**
 * Resolved identity of whoever hit the API.
 *
 * <ul>
 *   <li><b>WALLET_HOLDER</b> – authenticated via Bearer JWT.
 *       {@code identity} is the subject (user-id / phone) extracted from the token.
 *       {@code phoneNumber} is populated from the token claim; the caller cannot
 *       supply a different phone number.
 *   <li><b>THIRD_PARTY</b> – authenticated via API-Key + Client-Id headers.
 *       {@code identity} is the client-id.
 *       {@code phoneNumber} comes from the request body.
 * </ul>
 */
public record CallerContext(
        CallerType type,
        String     identity,
        String     phoneNumber
) {

    public static CallerContext walletHolder(String identity, String phoneNumber) {
        return new CallerContext(CallerType.WALLET_HOLDER, identity, phoneNumber);
    }

    public static CallerContext thirdParty(String clientId, String phoneNumber) {
        return new CallerContext(CallerType.THIRD_PARTY, clientId, phoneNumber);
    }

    public boolean isWalletHolder() { return type == CallerType.WALLET_HOLDER; }
    public boolean isThirdParty()   { return type == CallerType.THIRD_PARTY;   }
}
