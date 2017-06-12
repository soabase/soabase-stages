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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

class StagedFutureBuilderImpl<T> implements StagedFutureBuilder {
    private final Executor executor;
    private final Tracing tracing;

    StagedFutureBuilderImpl(Executor executor, Tracing tracing) {
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.tracing = tracing;
    }

    @Override
    public <U> StagedFutureTimeout<U> then(Supplier<U> proc) {
        return thenIf(() -> Optional.of(proc.get()));
    }

    @Override
    public <U> StagedFutureTimeout<U> thenIf(Supplier<Optional<U>> proc) {
        return new StagedFutureImpl<>(proc, executor, tracing);
    }

    @Override
    public <U> StagedFutureTimeout<U> thenStage(CompletionStage<Optional<U>> stage) {
        return new StagedFutureImpl<>(stage, executor, tracing);
    }
}
