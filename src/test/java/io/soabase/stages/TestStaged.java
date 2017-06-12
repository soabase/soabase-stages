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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStaged {
    private TestTracing tracing;

    @Before
    public void setup() {
        tracing = new TestTracing();
    }

    @After
    public void tearDown() {
        TestTracing.clearContext();
        tracing = null;
    }

    @Test
    public void testBasic() throws Exception {
        CompletionStage<List<TestTracing.Trace>> future = StagedFuture.sync(tracing)
            .then(() -> worker("1"))
            .then(s -> worker("2"))
            .then(s -> worker("3"))
            .whenCompleteYield(s -> tracing.getTracing());

        List<TestTracing.Trace> traces = future.toCompletableFuture().get(5, TimeUnit.SECONDS);
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
        CountDownLatch latch = new CountDownLatch(1);
        StagedFuture.sync(tracing)
            .then(() -> worker("1"))
            .then(s -> worker("2"))
            .thenIf(s -> Optional.empty())
            .whenAborted(latch::countDown);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        List<TestTracing.Trace> traces = tracing.getTracing();
        assertThat(traces).size().isEqualTo(6);
        for ( int i = 0; i < 4; i += 2 ) {
            String context = Integer.toString((i / 2) + 1);
            assertThat(traces.get(i).status).isEqualTo("start");
            assertThat(traces.get(i).context).isEqualTo(context);
            assertThat(traces.get(i + 1).status).isEqualTo("success");
            assertThat(traces.get(i + 1).context).isEqualTo(context);
        }
        assertThat(traces.get(4).status).isEqualTo("start");
        assertThat(traces.get(4).context).isNull();
        assertThat(traces.get(5).status).isEqualTo("success");
        assertThat(traces.get(5).context).isNull();
    }

    private String worker(String context) {
        TestTracing.setContext(context);
        tracing.resetLastContext(context);
        return context;
    }
}
