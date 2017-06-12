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

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Allows setting a timeout and an optional default value for the
 * task that was just set in the StagedFuture
 */
public interface TimeoutStagedFuture<T> extends StagedFuture<T> {
    /**
     * Sets a timeout for this stage's task. If the given timeout
     * elapses before the task completes this stage is completed
     * exceptionally with a {@link java.util.concurrent.TimeoutException}
     *
     * @param max max time for the task to execute
     * @return next stage in the chain
     */
    StagedFuture<T> withTimeout(Duration max);

    /**
     * Sets a timeout for this stage's task. If the given timeout
     * elapses before the task completes this stage is completed
     * with the given default value.
     *
     * @param max max time for the task to execute
     * @param defaultValue value to set if the task times out
     * @return next stage in the chain
     */
    StagedFuture<T> withTimeout(Duration max, Supplier<T> defaultValue);
}
