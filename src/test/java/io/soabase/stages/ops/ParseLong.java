package io.soabase.stages.ops;

import java.util.concurrent.CompletableFuture;

public class ParseLong extends CompletableFuture<Long> {
    public ParseLong(String s) {
        try {
            complete(Long.parseLong(s));
        } catch (Throwable e) {
            completeExceptionally(e);
        }
    }
}
