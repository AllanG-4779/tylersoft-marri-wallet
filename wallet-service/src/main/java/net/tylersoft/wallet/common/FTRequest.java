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
    private String creditAccount;
    private double amount;
    private boolean presentment;
    @Builder.Default
    private String transactionType = "FT";
    private String currency;
    private String phoneNumber;
}
