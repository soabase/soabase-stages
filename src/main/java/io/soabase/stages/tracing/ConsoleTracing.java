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
