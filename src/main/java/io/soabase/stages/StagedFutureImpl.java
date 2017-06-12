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

class StagedFutureImpl<T> implements StagedFuture<T> {
    private final Executor executor;
    private final CompletionStage<Optional<T>> future;
    private final Tracing tracing;
    private final Duration max;

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

    StagedFutureImpl(Supplier<Optional<T>> proc, Executor executor, Duration max, Tracing tracing) {
        this(
            Objects.requireNonNull(executor, "executor cannot be null"),
            CompletableFuture.supplyAsync(tracingProc(tracing, proc), executor),
            max,
            tracing,
            true
        );
    }

    StagedFutureImpl(CompletionStage<Optional<T>> future, Executor executor, Duration max, Tracing tracing) {
        this(
            Objects.requireNonNull(executor, "executor cannot be null"),
            future,
            max,
            tracing,
            true
        );
    }

    @Override
    public CompletionStage<Optional<T>> unwrap() {
        return future;
    }

    @Override
    public StagedFuture<T> withTimeout(Duration max) {
        return new StagedFutureImpl<>(executor, future, max, tracing, false);
    }

    @Override
    public <U> StagedFuture<U> thenIf(Function<T, Optional<U>> proc) {
        Objects.requireNonNull(proc, "proc cannot be null");

        Function<T, Optional<U>> tracedProc = tracingProc(tracing, proc);
        return new StagedFutureImpl<>(executor, future.thenApplyAsync(optional -> optional.flatMap(tracedProc), executor), max, tracing, true);
    }

    @Override
    public <U> StagedFuture<U> then(CompletionStage<Optional<U>> stage) {
        Objects.requireNonNull(stage, "stage cannot be null");
        // TODO needs testing - I'm not certain this does what's intended
        return new StagedFutureImpl<>(executor, future.thenComposeAsync(__ -> stage, executor), max, tracing, true);
    }

    @Override
    public <U> StagedFuture<U> then(Function<T, U> proc)
    {
        return thenIf(v -> Optional.of(proc.apply(v)));
    }

    @Override
    public StagedFutureTerminal<T> whenComplete(Consumer<T> handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        CompletionStage<Optional<T>> next = future.thenApply(optional -> {
            optional.ifPresent(handler);
            return optional;
        });
        return new StagedFutureImpl<>(executor, next, null, tracing, false);
    }

    @Override
    public StagedFutureTerminal<T> whenAbortedOrFailed(Consumer<Optional<Throwable>> handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        CompletionStage<Optional<T>> next = future.handle((optional, e) -> {
            if (e != null) {
                handler.accept(Optional.of(e));
                optional = Optional.empty();
            } else if (!optional.isPresent()) {
                handler.accept(Optional.empty());
            }
            return optional;
        });
        return new StagedFutureImpl<>(executor, next, null, tracing, false);
    }

    @Override
    public StagedFutureTerminal<T> whenAborted(Runnable handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        CompletionStage<Optional<T>> next = future.thenApply(optional -> {
            if ( !optional.isPresent() ) {
                handler.run();
            }
            return optional;
        });
        return new StagedFutureImpl<>(executor, next, null, tracing, false);
    }

    @Override
    public StagedFutureTerminal<T> whenFailed(Consumer<Throwable> handler) {
        CompletionStage<Optional<T>> next = future.exceptionally(e -> {
            handler.accept(e);
            return Optional.empty();
        });
        return new StagedFutureImpl<>(executor, next, null, tracing, false);
    }

    private StagedFutureImpl(Executor executor, CompletionStage<Optional<T>> future, Duration max, Tracing tracing, boolean applyMax) {
        this.executor = executor;
        this.max = max;
        this.future = (applyMax && (max != null)) ? Timeout.within(future, max) : future;
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
