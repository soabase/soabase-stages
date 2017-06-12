package io.soabase.stages;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface StagedFutureTerminal<T> {
    StagedFutureTerminal<T> whenComplete(Consumer<T> handler);

    StagedFutureTerminal<T> whenAborted(Runnable handler);

    StagedFutureTerminal<T> whenFailed(Consumer<Throwable> handler);

    StagedFutureTerminal<T> whenAbortedOrFailed(Consumer<Optional<Throwable>> handler);

    CompletionStage<Optional<T>> unwrap();
}
