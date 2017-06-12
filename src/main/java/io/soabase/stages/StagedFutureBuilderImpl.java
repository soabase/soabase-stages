package io.soabase.stages;

import io.soabase.stages.tracing.Tracing;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

class StagedFutureBuilderImpl<T> implements StagedFutureBuilder {
    private final Executor executor;
    private final Tracing tracing;
    private volatile Duration max;

    StagedFutureBuilderImpl(Executor executor, Tracing tracing) {
        this.executor = executor;
        this.tracing = tracing;
        max = null;
    }

    @Override
    public <U> StagedFuture<U> then(Supplier<U> proc) {
        return thenIf(() -> Optional.of(proc.get()));
    }

    @Override
    public <U> StagedFuture<U> thenIf(Supplier<Optional<U>> proc) {
        return new StagedFutureImpl<>(proc, executor, max, tracing);
    }

    @Override
    public <U> StagedFuture<U> then(CompletionStage<Optional<U>> stage) {
        return new StagedFutureImpl<>(stage, executor, max, tracing);
    }

    @Override
    public StagedFutureBuilder withTimeout(Duration max) {
        this.max = max;
        return this;
    }
}
