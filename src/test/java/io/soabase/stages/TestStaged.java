package io.soabase.stages;

import io.soabase.stages.tracing.Tracing;
import org.junit.Test;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.Executors;

public class TestStaged {
    @Test
    public void testBasic() throws Exception {
        StagedFutureTerminal<String> stagedFuture = StagedFuture.async(Executors.newCachedThreadPool(), Tracing.console())
            .then(() -> simulateFindFile("hey")).withTimeout(Duration.ofSeconds(5), () -> new File("timed-out"))
            .then(f -> simulateReadFile(f, true))
            .whenComplete(System.out::println)
            .whenFailed(Throwable::printStackTrace);
        stagedFuture.unwrap().toCompletableFuture().get();
    }

    private static File simulateFindFile(String aName) {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new File(aName);
    }

    private static String simulateReadFile(File f, boolean something) {
        return new File(f, Boolean.toString(something)).getPath();
    }
}
