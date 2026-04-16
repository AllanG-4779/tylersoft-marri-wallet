package net.tylersoft.wallet.service;

import reactor.core.publisher.Mono;

/**
 * A single step in a {@link TransactionPipeline}.
 *
 * <p>Each step receives the current {@link TransactionContext}, performs its work
 * (validation, enrichment, or persistence), and returns an updated context.
 *
 * <p>Steps should never throw exceptions for business rule violations — instead,
 * return {@code Mono.just(ctx.withFailure(code, message))} so the pipeline can
 * short-circuit cleanly. Reserve exceptions for truly unexpected infrastructure
 * failures.
 *
 * <p>Concrete steps are provided as methods on {@link TransactionSteps}.
 */
@FunctionalInterface
public interface TransactionStep {
    Mono<TransactionContext> execute(TransactionContext ctx);
}
