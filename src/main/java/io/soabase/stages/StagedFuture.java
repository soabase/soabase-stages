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

import io.soabase.stages.tracing.Tracing;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A facade that makes staged/pipelined CompletableFutures much easier to create and manage
 */
public interface StagedFuture<T> {
    /**
     * Start a StagedFuture that executes tasks synchronously in the calling thread.
     *
     * @return builder
     */
    static StagedFutureBuilder sync() {
        return sync(null);
    }

    /**
     * Start a StagedFuture that executes tasks asynchronously using the given executor.
     *
     * @param executor executor to use to run tasks
     * @return builder
     */
    static StagedFutureBuilder async(Executor executor) {
        return async(executor, null);
    }

    /**
     * Start a StagedFuture that executes tasks asynchronously using the
     * {@link java.util.concurrent.ForkJoinPool#commonPool()}
     *
     * @return builder
     */
    static StagedFutureBuilder asyncPool() {
        return async(StagedFutureImpl.asyncPool, null);
    }

    /**
     * Start a StagedFuture that executes tasks synchronously in the calling thread.
     * You can provide a tracer that wraps/traces all tasks.
     *
     * @param tracing the tracer
     * @return builder
     */
    static StagedFutureBuilder sync(Tracing tracing) {
        return async(Runnable::run, tracing);
    }

    /**
     * Start a StagedFuture that executes tasks asynchronously using the given executor.
     * You can provide a tracer that wraps/traces all tasks.
     *
     * @param executor executor to use to run tasks
     * @param tracing the tracer
     * @return builder
     */
    static StagedFutureBuilder async(Executor executor, Tracing tracing) {
        return new StagedFutureBuilderImpl(executor, tracing);
    }

    /**
     * Start a StagedFuture that executes tasks asynchronously using the
     * {@link java.util.concurrent.ForkJoinPool#commonPool()}
     * You can provide a tracer that wraps/traces all tasks.
     *
     * @param tracing the tracer
     * @return builder
     */
    static StagedFutureBuilder asyncPool(Tracing tracing) {
        return async(StagedFutureImpl.asyncPool, tracing);
    }

    /**
     * <p>
     * If the current stage completes successfully, execute the given task
     * synchronously or asynchronously depending on how the StagedFuture was built.
     * The given task receives the result of this stage's execution.
     * The given task returns an optional value that indicates whether or not the next stage
     * can execute. If {@link Optional#empty()} is returned, the entire StagedFuture
     * chain is considered to be aborted and no future tasks will execute. The
     * {@link #whenAborted(Runnable)} completer will get called.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link TimeoutStagedFuture} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param proc task to execute
     * @return next stage in the chain
     */
    <U> TimeoutStagedFuture<U> thenIf(Function<T, Optional<U>> proc);

    /**
     * <p>
     * If the current stage completes successfully, execute the given task
     * synchronously or asynchronously depending on how the StagedFuture was built.
     * The given task receives the result of this stage's execution.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link TimeoutStagedFuture} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param proc task to execute
     * @return next stage in the chain
     */
    <U> TimeoutStagedFuture<U> then(Function<T, U> proc);

    /**
     * <p>
     * If the current stage completes successfully, chain to the given CompletionStage
     * synchronously or asynchronously depending on how the StagedFuture was built.
     * </p>
     *
     * <p>
     * Note: the returned value is a {@link TimeoutStagedFuture} which allows
     * a timeout and an optional default to be set for the task.
     * </p>
     *
     * @param stage stage to chain to
     * @return next stage in the chain
     */
    <U> TimeoutStagedFuture<U> then(CompletionStage<Optional<U>> stage);

    /**
     * If the stage and any previous stages in the chain complete successfully, the handler is called with the resulting value.
     *
     * @param handler consumer for the value
     * @return next stage in the chain
     */
    StagedFuture<T> whenComplete(Consumer<T> handler);

    /**
     * If this stage or any previous stages in the chain return {@link Optional#empty()}
     * from {@link #thenIf(Function)}, the handler is called
     *
     * @param handler abort handler
     * @return next stage in the chain
     */
    StagedFuture<T> whenAborted(Runnable handler);

    /**
     * If this stage or any previous stages in the chain complete exceptionally, the handler is called
     *
     * @param handler exception handler
     * @return next stage in the chain
     */
    StagedFuture<T> whenFailed(Consumer<Throwable> handler);

    /**
     * Return the internally managed CompletionStage
     *
     * @return CompletionStage
     */
    CompletionStage<Optional<T>> unwrap();
}
