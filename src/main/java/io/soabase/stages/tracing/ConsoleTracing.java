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
package io.soabase.stages.tracing;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ConsoleTracing implements Tracing {
    private final Supplier<List<String>> contextProc;

    public ConsoleTracing(Supplier<List<String>> contextProc) {
        this.contextProc = Objects.requireNonNull(contextProc, "contextProc cannot be null");
    }

    @Override
    public void startProc() {
        System.out.println(Tracing.formatStart(contextProc));
    }

    @Override
    public void endProcSuccess(Duration duration) {
        System.out.println(Tracing.formatSuccess(contextProc, duration));
    }

    @Override
    public void endProcFail(Throwable e, Duration duration) {
        System.out.println(Tracing.formatFail(contextProc, e, duration));
    }
}
