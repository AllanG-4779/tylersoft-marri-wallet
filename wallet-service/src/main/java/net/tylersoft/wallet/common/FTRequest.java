package net.tylersoft.wallet.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FTRequest {
    private String debitAccount;
    @Builder.Default
    private String transactionCode = "FT";
    private String creditAccount;
    private double amount;
    private boolean presentment;
    @Builder.Default
    private String transactionType = "FT";
    private String currency;
    private String phoneNumber;
    /** Client-supplied transaction reference — used as transactionRef on TrxMessage when set. */
    private String transactionRef;
    /** Recipient phone number — used for airtime, bill payments, etc. */
    private String recipientPhoneNumber;
}
