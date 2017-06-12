package io.soabase.stages;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

// see https://dzone.com/articles/asynchronous-timeouts
public class Timeout {
    /**
     * The underlying thread only used to complete exceptionally. We just need the queued scheduler
     */
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("Timeout.FailAfter thread");
                    return thread;
                });

    /**
     * Return a new CompletionStage that either calls <code>completeExceptionally()</code> after
     * the given duration elapses or completes with the given future (exceptionally or normally).
     *
     * @param future main completion to wrap
     * @param duration wait time
     * @return new CompletionStage
     */
    public static <T> CompletionStage<T> within(CompletionStage<T> future, Duration duration) {
        final CompletionStage<T> timeout = internalFailAfter(duration, null, false);
        return future.applyToEither(timeout, Function.identity());
    }

    /**
     * Return a new CompletionStage that either calls<code>complete(defaultValue)</code> after
     * the given duration elapses or completes with the given future (exceptionally or normally).
     *
     * @param future main completion to wrap
     * @param duration wait time
     * @param defaultValue value to complete with on duration elapse
     * @return new CompletionStage
     */
    public static <T> CompletionStage<T> within(CompletionStage<T> future, Duration duration, Supplier<T> defaultValue) {
        final CompletionStage<T> timeout = internalFailAfter(duration, defaultValue, true);
        return future.applyToEither(timeout, Function.identity());
    }

    /**
     * Return a new CompletionStage that calls <code>completeExceptionally()</code> after
     * the given duration elapses.
     *
     * @param duration wait time
     * @return new CompletionStage
     */
    public static <T> CompletionStage<T> failAfter(Duration duration) {
        return internalFailAfter(duration, null, false);
    }

    /**
     * Return a new CompletionStage that calls <code>complete(defaultValue)</code> after
     * the given duration elapses.
     *
     * @param duration wait time
     * @param defaultValue value to complete with on duration elapse
     * @return new CompletionStage
     */
    public static <T> CompletionStage<T> failAfter(Duration duration, Supplier<T> defaultValue) {
        return internalFailAfter(duration, defaultValue, true);
    }

    private static <T> CompletionStage<T> internalFailAfter(Duration duration, Supplier<T> defaultValue, boolean useDefaultValue) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.schedule(() -> {
            final TimeoutException ex = new TimeoutException("Timeout after " + duration);
            if ( useDefaultValue ) {
                return future.complete(defaultValue.get());
            }
            return future.completeExceptionally(ex);
        }, duration.toMillis(), MILLISECONDS);
        return future;
    }

    private Timeout() {
    }
}
