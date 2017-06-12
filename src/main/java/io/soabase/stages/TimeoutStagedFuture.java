package io.soabase.stages;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Allows setting a timeout and an optional default value for the
 * task that was just set in the StagedFuture
 */
public interface TimeoutStagedFuture<T> extends StagedFuture<T> {
    /**
     * Sets a timeout for this stage's task. If the given timeout
     * elapses before the task completes this stage is completed
     * exceptionally with a {@link java.util.concurrent.TimeoutException}
     *
     * @param max max time for the task to execute
     * @return next stage in the chain
     */
    StagedFuture<T> withTimeout(Duration max);

    /**
     * Sets a timeout for this stage's task. If the given timeout
     * elapses before the task completes this stage is completed
     * with the given default value.
     *
     * @param max max time for the task to execute
     * @param defaultValue value to set if the task times out
     * @return next stage in the chain
     */
    StagedFuture<T> withTimeout(Duration max, Supplier<T> defaultValue);
}
