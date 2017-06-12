package io.soabase.stages;

import java.time.Duration;
import java.util.function.Supplier;

public interface TimeoutStagedFuture<T> extends StagedFuture<T> {
    StagedFuture<T> withTimeout(Duration max);

    StagedFuture<T> withTimeout(Duration max, Supplier<T> defaultValue);
}
