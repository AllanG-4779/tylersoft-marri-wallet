package net.tylersoft.wallet.common;

public enum CallerType {
    /** Authenticated wallet holder acting on their own account. */
    WALLET_HOLDER,
    /** Verified third-party system or integrator acting on behalf of a customer. */
    THIRD_PARTY
}
