package io.soabase.stages;

import io.soabase.stages.tracing.Tracing;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface StagedFuture<T> extends StagedFutureTerminal<T>, StagedFutureTimeout {
    static Executor asyncPool() {
        return StagedFutureImpl.asyncPool;
    }

    static StagedFutureBuilder sync() {
        return sync(null);
    }

    static StagedFutureBuilder sync(Tracing tracing) {
        return async(Runnable::run, tracing);
    }

    static StagedFutureBuilder async(Executor executor) {
        return async(executor, null);
    }

    static StagedFutureBuilder async(Executor executor, Tracing tracing) {
        return new StagedFutureBuilderImpl(executor, tracing);
    }

    <U> StagedFuture<U> thenIf(Function<T, Optional<U>> proc);

    <U> StagedFuture<U> then(Function<T, U> proc);

    <U> StagedFuture<U> then(CompletionStage<Optional<U>> stage);

    @Override
    StagedFuture<T> withTimeout(Duration max);

    @Override
    StagedFutureTerminal<T> whenComplete(Consumer<T> handler);

    @Override
    StagedFutureTerminal<T> whenAborted(Runnable handler);

    @Override
    StagedFutureTerminal<T> whenFailed(Consumer<Throwable> handler);

    @Override
    CompletionStage<Optional<T>> unwrap();
}
