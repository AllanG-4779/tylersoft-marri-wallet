package net.tylersoft.wallet.service;

import lombok.Builder;
import lombok.Getter;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.wallet.model.Account;
import net.tylersoft.wallet.model.ChargeConfig;
import net.tylersoft.wallet.model.ServiceManagement;
import net.tylersoft.wallet.model.TransactionEntry;
import net.tylersoft.wallet.model.TrxMessage;
import net.tylersoft.wallet.topup.CardDetails;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable context object that flows through every step of a
 * {@link TransactionPipeline}. Each step receives the context, validates or
 * enriches it, and returns a new instance via {@code toBuilder()}.
 *
 * <p>If a step encounters a business rule violation it calls
 * {@link #withFailure(String, String)} — this sets {@code failed = true} and
 * causes all subsequent steps to be skipped automatically by the pipeline.
 */
@Getter
@Builder(toBuilder = true)
public class TransactionContext {


    private final FTRequest request;

    // ── Step 1: Staging ───────────────────────────────────────────────────────
    /** The pending record written to {@code trx_messages}. */
    private final TrxMessage stagedMessage;

    // ── Step 2: Transaction validation ───────────────────────────────────────
    private final Account          debitAccount;
    private final Account          creditAccount;
    private final ServiceManagement serviceManagement;

    // ── Step 3: Charge validation ─────────────────────────────────────────────
    private final List<ChargeConfig> chargeConfigs;
    private final BigDecimal         totalCharge;

    // ── Step 5: Posting result ────────────────────────────────────────────────
    /** Debit and credit entries written to {@code trx_transaction_entries}. */
    private final List<TransactionEntry> entries;

    // ── Card topup extras ─────────────────────────────────────────────────────
    /** Card details supplied by the customer — only present for card topup flows. */
    private final CardDetails cardDetails;

    private final boolean failed;
    private final String  failureCode;
    private final String  failureMessage;

    public static TransactionContext from(FTRequest request) {
        return TransactionContext.builder()
                .request(request)
                .totalCharge(BigDecimal.ZERO)
                .failed(false)
                .build();
    }

    public TransactionContext withFailure(String code, String message) {
        return this.toBuilder()
                .failed(true)
                .failureCode(code)
                .failureMessage(message)
                .build();
    }

    public boolean isSuccessful() {
        return !failed;
    }
}
