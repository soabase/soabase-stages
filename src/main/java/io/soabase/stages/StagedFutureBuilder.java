package io.soabase.stages;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface StagedFutureBuilder extends StagedFutureTimeout {
    <U> StagedFuture<U> thenIf(Supplier<Optional<U>> proc);

    <U> StagedFuture<U> then(CompletionStage<Optional<U>> stage);

    <U> StagedFuture<U> then(Supplier<U> proc);

    @Override
    StagedFutureBuilder withTimeout(Duration max);
}
