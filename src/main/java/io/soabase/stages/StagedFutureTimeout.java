package io.soabase.stages;

import java.time.Duration;

public interface StagedFutureTimeout {
    Object withTimeout(Duration max);
}
