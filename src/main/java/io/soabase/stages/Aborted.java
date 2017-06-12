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

public class Aborted {
    public static class AbortException extends Exception {
        public AbortException() {
            super("Aborted");
        }
    }

    public static <T, U> CompletionStage<Optional<U>> whenComplete(CompletionStage<Optional<T>> stage, Function<T, Optional<U>> handler)
    {
        return stage.thenApply(optional -> optional.flatMap(handler));
    }

    public static <T, U> CompletionStage<Optional<U>> whenCompleteAsync(CompletionStage<Optional<T>> stage, Function<T, Optional<U>> handler)
    {
        return stage.thenApplyAsync(optional -> optional.flatMap(handler));
    }

    public static <T, U> CompletionStage<Optional<U>> whenCompleteAsync(CompletionStage<Optional<T>> stage, Function<T, Optional<U>> handler, Executor executor)
    {
        return stage.thenApplyAsync(optional -> optional.flatMap(handler), executor);
    }

    public static <T, U> CompletionStage<Optional<U>> whenAborted(CompletionStage<Optional<T>> stage, Supplier<Optional<U>> handler)
    {
        return stage.thenApply(optional -> optional.isPresent() ? Optional.empty() : handler.get());
    }

    public static <T, U> CompletionStage<Optional<U>> whenAbortedAsync(CompletionStage<Optional<T>> stage, Supplier<Optional<U>> handler)
    {
        return stage.thenApplyAsync(optional -> optional.isPresent() ? Optional.empty() : handler.get());
    }

    public static <T, U> CompletionStage<Optional<U>> whenAbortedAsync(CompletionStage<Optional<T>> stage, Supplier<Optional<U>> handler, Executor executor)
    {
        return stage.thenApplyAsync(optional -> optional.isPresent() ? Optional.empty() : handler.get());
    }
}
