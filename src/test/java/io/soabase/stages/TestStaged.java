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

import io.soabase.stages.tracing.Cancelable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStaged {
    private TestTracing tracing;
    private ExecutorService executor;

    @Before
    public void setup() {
        tracing = new TestTracing();
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        };
        executor = Executors.newCachedThreadPool(threadFactory);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        executor = null;
        TestTracing.clearContext();
        tracing = null;
    }

    @Test
    public void testBasic() throws Exception {
        StagedFuture<List<TestTracing.Trace>> future = StagedFuture.sync(tracing)
            .thenIf(() -> worker("1"))
            .thenIf(s -> worker("2"))
            .thenIf(s -> worker("3"))
            .whenSucceededYield(s -> tracing.getTracing());

        Optional<List<TestTracing.Trace>> optional = complete(future);
        assertThat(optional).isPresent();
        //noinspection ConstantConditions
        List<TestTracing.Trace> traces = optional.get();
        assertThat(traces).isNotNull();
        assertThat(traces).size().isEqualTo(6);
        for ( int i = 0; i < 6; i += 2 ) {
            String context = Integer.toString((i / 2) + 1);
            assertThat(traces.get(i).status).isEqualTo("start");
            assertThat(traces.get(i).context).isEqualTo(context);
            assertThat(traces.get(i + 1).status).isEqualTo("success");
            assertThat(traces.get(i + 1).context).isEqualTo(context);
        }
    }

    @Test
    public void testAbort() throws Exception {
        AtomicBoolean isAborted = new AtomicBoolean(false);
        complete(StagedFuture.sync(tracing)
            .thenIf(() -> worker("1"))
            .thenIf(s -> Optional.empty())
            .thenIf(s -> worker("2"))
            .thenIf(s -> worker("3"))
            .thenIf(s -> worker("4"))
            .thenIf(s -> worker("5"))
            .thenIf(s -> worker("6"))
            .whenAborted(() -> isAborted.set(true)));

        assertThat(isAborted.get()).isTrue();

        List<TestTracing.Trace> traces = tracing.getTracing();
        assertThat(traces).size().isEqualTo(4);
        assertThat(traces.get(0).status).isEqualTo("start");
        assertThat(traces.get(0).context).isEqualTo("1");
        assertThat(traces.get(1).status).isEqualTo("success");
        assertThat(traces.get(1).context).isEqualTo("1");
        assertThat(traces.get(2).status).isEqualTo("start");
        assertThat(traces.get(2).context).isNull();
        assertThat(traces.get(3).status).isEqualTo("success");
        assertThat(traces.get(3).context).isNull();
    }

    @Test
    public void testTimeout() throws Exception {
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        complete(StagedFuture.async(executor, tracing)
            .thenIf(() -> worker("1"))
            .thenIf(s -> hangingWorker("2")).withTimeout(Duration.ofSeconds(2))
            .thenIf(s -> worker("3"))
            .thenIf(s -> worker("4"))
            .whenFailed(e -> {
                while ( e instanceof CompletionException ) {
                    e = e.getCause();
                }
                if (e instanceof TimeoutException) {
                    isTimeout.set(true);
                }
            }));

        assertThat(isTimeout.get()).isTrue();

        List<TestTracing.Trace> traces = tracing.getTracing();
        assertThat(traces).size().isEqualTo(3);
        assertThat(traces.get(0).status).isEqualTo("start");
        assertThat(traces.get(0).context).isEqualTo("1");
        assertThat(traces.get(1).status).isEqualTo("success");
        assertThat(traces.get(1).context).isEqualTo("1");
        assertThat(traces.get(2).status).isEqualTo("start");
        assertThat(traces.get(2).context).isEqualTo("2");
    }

    @Test
    public void testTimeoutAndDefault() throws Exception {
        complete(StagedFuture.async(executor, tracing)
            .thenIf(() -> worker("1"))
            .thenIf(s -> hangingWorker("2")).withTimeout(Duration.ofSeconds(2), () -> "default")
            .thenIf(this::worker)
            .thenIf(s -> worker("4")));

        List<TestTracing.Trace> traces = tracing.getTracing();
        assertThat(traces).size().isEqualTo(7);
        assertThat(traces.get(0).status).isEqualTo("start");
        assertThat(traces.get(0).context).isEqualTo("1");
        assertThat(traces.get(1).status).isEqualTo("success");
        assertThat(traces.get(1).context).isEqualTo("1");
        assertThat(traces.get(2).status).isEqualTo("start");
        assertThat(traces.get(2).context).isEqualTo("2");
        assertThat(traces.get(3).status).isEqualTo("start");
        assertThat(traces.get(3).context).isEqualTo("default");
        assertThat(traces.get(4).status).isEqualTo("success");
        assertThat(traces.get(4).context).isEqualTo("default");
        assertThat(traces.get(5).status).isEqualTo("start");
        assertThat(traces.get(5).context).isEqualTo("4");
        assertThat(traces.get(6).status).isEqualTo("success");
        assertThat(traces.get(6).context).isEqualTo("4");
    }

    @Test
    public void testFailure() throws Exception {
        AtomicReference<String> exceptionMessage = new AtomicReference<>();
        complete(StagedFuture.async(executor, tracing)
            .thenIf(() -> worker("1"))
            .thenIf(s -> failureWorker("2"))
            .thenIf(s -> worker("3"))
            .thenIf(s -> worker("4"))
            .whenFailed(e -> {
                while ( e instanceof CompletionException ) {
                    e = e.getCause();
                }
                exceptionMessage.set(e.getMessage());
            }));

        assertThat(exceptionMessage.get()).isEqualTo("2");

        List<TestTracing.Trace> traces = tracing.getTracing();
        assertThat(traces).size().isEqualTo(4);
        assertThat(traces.get(0).status).isEqualTo("start");
        assertThat(traces.get(0).context).isEqualTo("1");
        assertThat(traces.get(1).status).isEqualTo("success");
        assertThat(traces.get(1).context).isEqualTo("1");
        assertThat(traces.get(2).status).isEqualTo("start");
        assertThat(traces.get(2).context).isEqualTo("2");
        assertThat(traces.get(3).status).isEqualTo("fail");
        assertThat(traces.get(3).context).isEqualTo("2");
    }

    @Test
    public void testCancelable() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        Cancelable cancelable = new Cancelable(tracing);
        StagedFuture<String> staged = StagedFuture.async(executor, cancelable)
            .thenIf(() -> worker("1"))
            .thenIf(s -> {
                latch.countDown();
                try {
                    return hangingWorker("2");
                } catch (RuntimeException e) {
                    if ( Thread.currentThread().isInterrupted() ) {
                        wasInterrupted.set(true);
                    }
                    throw e;
                }
            })
            .thenIf(s -> worker("3"))
            .thenIf(s -> worker("4"));

        latch.await();

        cancelable.cancelChain(true);
        try {
            complete(staged);
            Assert.fail("Should have thrown");
        } catch (Exception e) {
            assertThat(e.getCause()).isNotNull();
            assertThat(e.getCause().getCause()).isNotNull();
            assertThat(e.getCause().getCause()).isInstanceOf(InterruptedException.class);
        }
    }

    private <T> Optional<T> complete(StagedFuture<T> stagedFuture) throws Exception {
        return complete(stagedFuture.unwrap());
    }

    private <T> Optional<T> complete(CompletionStage<Optional<T>> stage) throws Exception {
        return stage.toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private Optional<String> worker(String context) {
        TestTracing.setContext(context);
        tracing.resetLastContext(context);
        return Optional.of(context);
    }

    private Optional<String> failureWorker(@SuppressWarnings("SameParameterValue") String context) {
        TestTracing.setContext(context);
        tracing.resetLastContext(context);
        throw new RuntimeException(context);
    }

    private Optional<String> hangingWorker(@SuppressWarnings("SameParameterValue") String context) {
        TestTracing.setContext(context);
        tracing.resetLastContext(context);
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return Optional.of(context);
    }
}
