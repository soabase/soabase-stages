package io.soabase.stages;

import io.soabase.stages.tracing.Tracing;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

class StagedFutureBuilderImpl<T> implements StagedFutureBuilder {
    private final Executor executor;
    private final Tracing tracing;

    StagedFutureBuilderImpl(Executor executor, Tracing tracing) {
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.tracing = tracing;
    }

    @Override
    public <U> TimeoutStagedFuture<U> then(Supplier<U> proc) {
        return thenIf(() -> Optional.of(proc.get()));
    }

    @Override
    public <U> TimeoutStagedFuture<U> thenIf(Supplier<Optional<U>> proc) {
        return new StagedFutureImpl<>(proc, executor, tracing);
    }

    @Override
    public <U> TimeoutStagedFuture<U> then(CompletionStage<Optional<U>> stage) {
        return new StagedFutureImpl<>(stage, executor, tracing);
    }
}
