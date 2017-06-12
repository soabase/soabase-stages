package io.soabase.stages;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Sets the first task in the chain
 */
public interface StagedFutureBuilder {
    /**
     * <p>
     * Execute the given task synchronously or asynchronously depending on how the StagedFuture was built.
     * The given task returns an optional value that indicates whether or not the next stage
     * can execute. If {@link Optional#empty()} is returned, the entire StagedFuture
     * chain is considered to be aborted and no future tasks will execute. The
     * {@link StagedFuture#whenAborted(Runnable)} completer will get called.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link TimeoutStagedFuture} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param proc task to execute
     * @return next stage in the chain
     */
    <U> TimeoutStagedFuture<U> thenIf(Supplier<Optional<U>> proc);

    /**
     * <p>
     * Execute the given task synchronously or asynchronously depending on how the StagedFuture was built.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link TimeoutStagedFuture} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param proc task to execute
     * @return next stage in the chain
     */
    <U> TimeoutStagedFuture<U> then(Supplier<U> proc);

    /**
     * <p>
     * Use the given CompletionStage as the initial stage for this StagedFuture.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link TimeoutStagedFuture} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param stage first stage
     * @return next stage in the chain
     */
    <U> TimeoutStagedFuture<U> then(CompletionStage<Optional<U>> stage);
}
