package net.tylersoft.wallet.service;

import net.tylersoft.wallet.common.FTRequest;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Reactive pipeline that runs a sequence of {@link TransactionStep}s against a
 * shared {@link TransactionContext}.
 *
 * <p>Steps are executed sequentially via {@code flatMap}. If any step marks the
 * context as failed — by calling {@link TransactionContext#withFailure} — all
 * remaining steps are skipped and the failed context is returned as the result.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Build once (e.g. in a @PostConstruct or as a @Bean)
 * TransactionPipeline fundTransfer = TransactionPipeline.builder()
 *         .step(steps.staging())
 *         .step(steps.validateTransaction())
 *         .step(steps.validateCharges())
 *         .step(steps.validateLimits())
 *         .step(steps.post())
 *         .build();
 *
 * // Execute per request
 * fundTransfer.run(request)
 *         .flatMap(ctx -> ctx.isSuccessful()
 *                 ? Mono.just(ApiResponse.ok(ctx.getStagedMessage().getTransactionRef()))
 *                 : Mono.just(ApiResponse.error(ctx.getFailureCode() + " - " + ctx.getFailureMessage())));
 * }</pre>
 */
public class TransactionPipeline {

    private final List<TransactionStep> steps;
    private final TransactionStep       onFailure;

    private TransactionPipeline(List<TransactionStep> steps, TransactionStep onFailure) {
        this.steps     = List.copyOf(steps);
        this.onFailure = onFailure;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs the pipeline for the given request.
     *
     * <p>Steps execute sequentially. If any step marks the context as failed,
     * all remaining steps are skipped and the {@code onFailure} hook (if set)
     * is called — giving it a chance to persist the failure back to the DB.
     *
     * <p>Returns a {@link Mono} that always emits a {@link TransactionContext}
     * — never an error signal — whether the pipeline succeeded or failed.
     */
    public Mono<TransactionContext> run(FTRequest request) {
        return run(TransactionContext.from(request));
    }

    /**
     * Runs the pipeline starting from a pre-built context — used by flows (e.g. card topup)
     * that need to seed the context with additional data before execution begins.
     */
    public Mono<TransactionContext> run(TransactionContext initialCtx) {
        Mono<TransactionContext> pipeline = Mono.just(initialCtx);

        for (TransactionStep step : steps) {
            pipeline = pipeline.flatMap(ctx ->
                    ctx.isFailed() ? Mono.just(ctx) : step.execute(ctx));
        }

        // Always run the failure hook at the end if the context is failed
        if (onFailure != null) {
            pipeline = pipeline.flatMap(ctx ->
                    ctx.isFailed() ? onFailure.execute(ctx) : Mono.just(ctx));
        }

        return pipeline;
    }

    public static class Builder {
        private final List<TransactionStep> steps = new ArrayList<>();
        private TransactionStep onFailure;

        public Builder step(TransactionStep step) {
            steps.add(step);
            return this;
        }

        /** Step invoked automatically when any pipeline step fails. */
        public Builder onFailure(TransactionStep onFailure) {
            this.onFailure = onFailure;
            return this;
        }

        public TransactionPipeline build() {
            return new TransactionPipeline(steps, onFailure);
        }
    }
}
