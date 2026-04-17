package net.tylersoft.users.common;

import javax.tools.Tool;

public enum CustomerStatus {
    /** Record created, OTP not yet verified. */
    INITIATED,
    /** Phone number confirmed via OTP. */
    PHONE_VERIFIED,
    /** Identity documents uploaded, awaiting KYC review. */
    DOCUMENTS_UPLOADED,
    /** KYC passed — customer may now open a wallet account. */
    KYC_VERIFIED,
    /** Fully onboarded and operational. */
    ACTIVE,
    SUSPENDED,
    CLOSED,
     WALLET_CREATION_FAILED
}
