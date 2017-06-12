package io.soabase.stages.ops;

import java.util.concurrent.CompletableFuture;

public class AString extends CompletableFuture<String> {
    public AString(String theThing) {
        complete(theThing);
    }
}
