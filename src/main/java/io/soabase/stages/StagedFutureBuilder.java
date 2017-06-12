package io.soabase.stages;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface StagedFutureBuilder {
    <U> TimeoutStagedFuture<U> thenIf(Supplier<Optional<U>> proc);

    <U> TimeoutStagedFuture<U> then(CompletionStage<Optional<U>> stage);

    <U> TimeoutStagedFuture<U> then(Supplier<U> proc);
}
