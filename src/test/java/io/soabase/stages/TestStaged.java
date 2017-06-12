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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
        StagedFuture<List<TestTracing.Trace>> future = StagedFuture.sync(tracing)
            .then(() -> worker("1"))
            .then(s -> worker("2"))
            .then(s -> worker("3"))
            .whenCompleteYield(s -> tracing.getTracing());

        Optional<List<TestTracing.Trace>> optional = future.unwrap().toCompletableFuture().get(5, TimeUnit.SECONDS);
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
        StagedFuture.sync(tracing)
            .then(() -> worker("1"))
            .thenIf(s -> Optional.empty())
            .then(s -> worker("2"))
            .then(s -> worker("3"))
            .then(s -> worker("4"))
            .then(s -> worker("5"))
            .then(s -> worker("6"))
            .whenAborted(() -> isAborted.set(true))
            .unwrap().toCompletableFuture().get(5, TimeUnit.SECONDS)
        ;

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

    private String worker(String context) {
        TestTracing.setContext(context);
        tracing.resetLastContext(context);
        return context;
    }
}
