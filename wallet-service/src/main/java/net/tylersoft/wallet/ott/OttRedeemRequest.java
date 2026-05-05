package net.tylersoft.wallet.ott;

import jakarta.validation.constraints.NotBlank;

public record OttRedeemRequest(
        @NotBlank String voucherPin,
        @NotBlank String creditAccount,  // wallet account to credit
        @NotBlank String currency,
        String amount                    // null or "0" → full voucher redemption
) {}
