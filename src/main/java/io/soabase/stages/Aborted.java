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
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wrappers that support aborting stages via empty Optionals.
 */
public class Aborted {
    public static class AbortException extends Exception {
        public AbortException() {
            super("Aborted");
        }
    }

    /**
     * Return a new CompletionStage that calls the given handler iff the given stage
     * completes successfully with an {@link Optional} that is not empty.
     *
     * @param stage the stage to wrap
     * @param handler success handler - called only when the given stage returns a non-empty Optional
     * @return new CompletionStage
     */
    public static <T, U> CompletionStage<Optional<U>> whenComplete(CompletionStage<Optional<T>> stage, Function<T, Optional<U>> handler)
    {
        return stage.thenApply(optional -> optional.flatMap(handler));
    }

    /**
     * Return a new CompletionStage that calls the given handler iff the given stage
     * completes successfully, executed using this stage's default asynchronous execution facility, with
     * an {@link Optional} that is not empty.
     *
     * @param stage the stage to wrap
     * @param handler success handler - called only when the given stage returns a non-empty Optional
     * @return new CompletionStage
     */
    public static <T, U> CompletionStage<Optional<U>> whenCompleteAsync(CompletionStage<Optional<T>> stage, Function<T, Optional<U>> handler)
    {
        return stage.thenApplyAsync(optional -> optional.flatMap(handler));
    }

    /**
     * Return a new CompletionStage that calls the given handler iff the given stage
     * completes successfully, executed using the given executor, with
     * an {@link Optional} that is not empty.
     *
     * @param stage the stage to wrap
     * @param handler success handler - called only when the given stage returns a non-empty Optional
     * @param executor the executor to use for asynchronous execution
     * @return new CompletionStage
     */
    public static <T, U> CompletionStage<Optional<U>> whenCompleteAsync(CompletionStage<Optional<T>> stage, Function<T, Optional<U>> handler, Executor executor)
    {
        return stage.thenApplyAsync(optional -> optional.flatMap(handler), executor);
    }

    /**
     * Return a new CompletionStage that calls the given handler iff the given stage
     * completes successfully with an {@link Optional} that is empty.
     *
     * @param stage the stage to wrap
     * @param handler success handler - called only when the given stage returns an empty Optional
     * @return new CompletionStage
     */
    public static <T, U> CompletionStage<Optional<U>> whenAborted(CompletionStage<Optional<T>> stage, Supplier<Optional<U>> handler)
    {
        return stage.thenApply(optional -> optional.isPresent() ? Optional.empty() : handler.get());
    }

    /**
     * Return a new CompletionStage that calls the given handler iff the given stage
     * completes successfully, executed using this stage's default asynchronous execution facility, with
     * an {@link Optional} that is empty.
     *
     * @param stage the stage to wrap
     * @param handler success handler - called only when the given stage returns an empty Optional
     * @return new CompletionStage
     */
    public static <T, U> CompletionStage<Optional<U>> whenAbortedAsync(CompletionStage<Optional<T>> stage, Supplier<Optional<U>> handler)
    {
        return stage.thenApplyAsync(optional -> optional.isPresent() ? Optional.empty() : handler.get());
    }

    /**
     * Return a new CompletionStage that calls the given handler iff the given stage
     * completes successfully, executed using the given executor, with
     * an {@link Optional} that is empty.
     *
     * @param stage the stage to wrap
     * @param handler success handler - called only when the given stage returns an empty Optional
     * @param executor the executor to use for asynchronous execution
     * @return new CompletionStage
     */
    public static <T, U> CompletionStage<Optional<U>> whenAbortedAsync(CompletionStage<Optional<T>> stage, Supplier<Optional<U>> handler, Executor executor)
    {
        return stage.thenApplyAsync(optional -> optional.isPresent() ? Optional.empty() : handler.get());
    }
}
