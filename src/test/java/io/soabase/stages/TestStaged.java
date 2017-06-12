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
import org.junit.Test;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.Executors;

public class TestStaged {
    @Test
    public void testBasic() throws Exception {
        StagedFuture<String> stagedFuture = StagedFuture.async(Executors.newCachedThreadPool(), Tracing.console())
            .then(() -> simulateFindFile("hey")).withTimeout(Duration.ofSeconds(5))//, () -> new File("timed-out"))
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
