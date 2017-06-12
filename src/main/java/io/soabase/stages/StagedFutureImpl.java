/**
 * Copyright 2017 Jordan Zimmerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

class StagedFutureImpl<T> implements StagedFuture<T>, StagedFutureTimeout<T> {
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
    public <U> StagedFutureTimeout<U> thenIf(Function<T, Optional<U>> proc) {
        Objects.requireNonNull(proc, "proc cannot be null");

        // don't burn a thread if the optional is empty
        CompletionStage<Optional<U>> nextStage = future.thenCompose(optional -> {
            if (optional.isPresent()) {
                Function<T, Optional<U>> tracedProc = tracingProc(tracing, proc);
                return future.thenApplyAsync(__ -> tracedProc.apply(optional.get()), executor);
            }
            return CompletableFuture.completedFuture(Optional.empty());
        });
        return new StagedFutureImpl<>(executor, nextStage, tracing);
    }

    @Override
    public <U> StagedFutureTimeout<U> then(Function<T, U> proc)
    {
        return thenIf(v -> Optional.of(proc.apply(v)));
    }

    @Override
    public <U> StagedFutureTimeout<U> thenStageIf(Function<Optional<T>, CompletionStage<Optional<U>>> stage) {
        Objects.requireNonNull(stage, "stage cannot be null");
        return new StagedFutureImpl<>(executor, future.thenComposeAsync(stage, executor), tracing);
    }

    @Override
    public <U> StagedFutureTimeout<U> thenStage(Function<T, CompletionStage<U>> stage) {
        Objects.requireNonNull(stage, "stage cannot be null");
        CompletionStage<Optional<U>> stageIf = future.thenComposeAsync(optional -> {
            if ( optional.isPresent() ) {
                CompletionStage<U> applied = stage.apply(optional.get());
                return applied.thenApplyAsync(Optional::of, executor);
            }

            return CompletableFuture.completedFuture(Optional.empty());
        }, executor);
        return new StagedFutureImpl<>(executor, stageIf, tracing);
    }

    @Override
    public StagedFuture<T> withTimeout(Duration max) {
        CompletionStage<Optional<T>> timeout = Timeout.within(future, max);
        return new StagedFutureImpl<>(executor, timeout, tracing);
    }

    @Override
    public StagedFuture<T> withTimeout(Duration max, Supplier<T> defaultValue) {
        CompletionStage<Optional<T>> timeout = Timeout.within(future, max, () -> Optional.of(defaultValue.get()));
        return new StagedFutureImpl<>(executor, timeout, tracing);
    }

    @Override
    public StagedFuture<T> whenComplete(Consumer<T> handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        return whenCompleteYield(value -> {
            handler.accept(value);
            return value;
        });
    }

    @Override
    public <U> StagedFuture<U> whenCompleteYield(Function<T, U> handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        CompletionStage<Optional<U>> next = Aborted.whenCompleteAsync(future, value -> Optional.of(handler.apply(value)));
        return new StagedFutureImpl<>(executor, next, tracing);
    }

    @Override
    public StagedFuture<T> whenAborted(Runnable handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        CompletionStage<Optional<T>> wrapped = Aborted.whenAbortedAsync(future, () -> {
            handler.run();
            return Optional.empty();
        }, executor);
        return new StagedFutureImpl<>(executor, wrapped, tracing);
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

    @Override
    public StagedFuture<T> whenFinal(Runnable handler) {
        CompletionStage<Optional<T>> next = future.handleAsync((value, __) -> {
            handler.run();
            return (value != null) ? value : Optional.empty();
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
