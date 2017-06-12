package io.soabase.stages;

import io.soabase.stages.tracing.Tracing;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class StagedFutureImpl<T> implements StagedFuture<T>, TimeoutStagedFuture<T> {
    private final Executor executor;
    private final CompletionStage<Optional<T>> future;
    private final Tracing tracing;

    private static final boolean useCommonPool =
        (ForkJoinPool.getCommonPoolParallelism() > 1);

    /**
     * Default executor -- ForkJoinPool.commonPool() unless it cannot
     * support parallelism.
     */
    static final Executor asyncPool = useCommonPool ?
        ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();

    /** Fallback if ForkJoinPool.commonPool() cannot support parallelism */
    private static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) { new Thread(r).start(); }
    }

    StagedFutureImpl(Supplier<Optional<T>> proc, Executor executor, Tracing tracing) {
        this(
            executor,
            CompletableFuture.supplyAsync(tracingProc(tracing, proc), executor),
            tracing
        );
    }

    StagedFutureImpl(CompletionStage<Optional<T>> future, Executor executor, Tracing tracing) {
        this(
            executor,
            future,
            tracing
        );
    }

    @Override
    public CompletionStage<Optional<T>> unwrap() {
        return future;
    }

    @Override
    public <U> TimeoutStagedFuture<U> thenIf(Function<T, Optional<U>> proc) {
        Objects.requireNonNull(proc, "proc cannot be null");

        Function<T, Optional<U>> tracedProc = tracingProc(tracing, proc);
        return new StagedFutureImpl<>(executor, future.thenApplyAsync(optional -> optional.flatMap(tracedProc), executor), tracing);
    }

    @Override
    public <U> TimeoutStagedFuture<U> then(CompletionStage<Optional<U>> stage) {
        Objects.requireNonNull(stage, "stage cannot be null");
        // TODO needs testing - I'm not certain this does what's intended
        return new StagedFutureImpl<>(executor, future.thenComposeAsync(__ -> stage, executor), tracing);
    }

    @Override
    public <U> TimeoutStagedFuture<U> then(Function<T, U> proc)
    {
        return thenIf(v -> Optional.of(proc.apply(v)));
    }

    @Override
    public StagedFuture<T> withTimeout(Duration max) {
        CompletionStage<Optional<T>> timeout = Timeout.within(future, max);
        return new StagedFutureImpl<>(executor, timeout, tracing);
    }

    @Override
    public StagedFuture<T> withTimeout(Duration max, Supplier<T> defaultValue) {
        CompletionStage<Optional<T>> timeout = Timeout.within(future, max, () -> Optional.ofNullable(defaultValue.get()));
        return new StagedFutureImpl<>(executor, timeout, tracing);
    }

    @Override
    public StagedFuture<T> whenComplete(Consumer<T> handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        CompletionStage<Optional<T>> next = future.thenApplyAsync(optional -> {
            optional.ifPresent(handler);
            return optional;
        }, executor);
        return new StagedFutureImpl<>(executor, next, tracing);
    }

    @Override
    public StagedFuture<T> whenAborted(Runnable handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        CompletionStage<Optional<T>> next = future.thenApplyAsync(optional -> {
            if ( !optional.isPresent() ) {
                handler.run();
            }
            return optional;
        }, executor);
        return new StagedFutureImpl<>(executor, next, tracing);
    }

    @Override
    public StagedFuture<T> whenFailed(Consumer<Throwable> handler) {
        CompletionStage<Optional<T>> next = future.handleAsync((__, e) -> {
            if ( e != null ) {
                handler.accept(e);
            }
            return Optional.empty();
        }, executor);
        return new StagedFutureImpl<>(executor, next, tracing);
    }

    private StagedFutureImpl(Executor executor, CompletionStage<Optional<T>> future, Tracing tracing) {
        this.executor = executor;
        this.future = future;
        this.tracing = tracing;
    }

    private static <U> Optional<U> trace(Tracing tracing, Supplier<Optional<U>> proc) {
        Instant start = Instant.now();
        tracing.startProc();
        try {
            Optional<U> result = proc.get();
            tracing.endProcSuccess(Duration.between(start, Instant.now()));
            return result;
        } catch (Throwable e) {
            tracing.endProcFail(e, Duration.between(start, Instant.now()));
            throw e;
        }
    }

    private static <T, U> Function<T, Optional<U>> tracingProc(Tracing tracing, Function<T, Optional<U>> proc) {
        Objects.requireNonNull(proc, "proc cannot be null");

        if ( tracing == null ) {
            return proc;
        }

        return value -> trace(tracing, () -> proc.apply(value));
    }

    private static <T> Supplier<Optional<T>> tracingProc(Tracing tracing, Supplier<Optional<T>> proc) {
        Objects.requireNonNull(proc, "proc cannot be null");

        if ( tracing == null ) {
            return proc;
        }

        return () -> trace(tracing, proc);
    }
}
