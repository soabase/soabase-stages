package io.soabase.stages;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AbortWrapper {
    public static class AbortException extends Exception {
        public AbortException() {
            super("Aborted");
        }
    }

    public static <T> CompletionStage<T> abortIfEmpty(CompletionStage<Optional<T>> stage) {
        return stage.thenCompose(opt -> {
            CompletableFuture<T> future = new CompletableFuture<>();
            if ( opt.isPresent() ) {
                future.complete(opt.get());
            } else {
                future.completeExceptionally(new AbortException());
            }
            return future;
        });
    }
}
