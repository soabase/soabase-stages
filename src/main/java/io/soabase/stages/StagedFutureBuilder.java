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

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Sets the first task in the chain
 */
public interface StagedFutureBuilder {
    /**
     * <p>
     * Execute the given task synchronously or asynchronously depending on how the StagedFuture was built.
     * The given task returns an optional value that indicates whether or not the next stage
     * can execute. If {@link Optional#empty()} is returned, the entire StagedFuture
     * chain is considered to be aborted and no future tasks will execute. The
     * {@link StagedFuture#whenAborted(Runnable)} completer will get called.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link StagedFutureTimeout} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param proc task to execute
     * @return next stage in the chain
     */
    <U> StagedFutureTimeout<U> thenIf(Supplier<Optional<U>> proc);

    /**
     * <p>
     * Execute the given task synchronously or asynchronously depending on how the StagedFuture was built.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link StagedFutureTimeout} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * <p>
     * Important: Procs that return <code>null</code> are not supported
     * </p>
     *
     * @param proc task to execute
     * @return next stage in the chain
     */
    <U> StagedFutureTimeout<U> then(Supplier<U> proc);

    /**
     * <p>
     * Use the given CompletionStage as the initial stage for this StagedFuture.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link StagedFutureTimeout} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param stage first stage
     * @return next stage in the chain
     */
    <U> StagedFutureTimeout<U> thenStageIf(CompletionStage<Optional<U>> stage);

    /**
     * <p>
     * Use the given CompletionStage as the initial stage for this StagedFuture.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link StagedFutureTimeout} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param stage first stage
     * @return next stage in the chain
     */
    <U> StagedFutureTimeout<U> thenStage(CompletionStage<U> stage);
}
