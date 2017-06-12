package io.soabase.stages;

import io.soabase.stages.tracing.Tracing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestTracing implements Tracing {
    private static final ThreadLocal<String> context = new ThreadLocal<>();
    private final List<Trace> tracing = new CopyOnWriteArrayList<>();

    public static class Trace {
        public final String status;
        public final String context;
        public final Duration duration;
        public final Throwable e;

        public Trace(String status, String context, Duration duration, Throwable e) {
            this.status = status;
            this.context = context;
            this.duration = duration;
            this.e = e;
        }
    }

    public List<Trace> getTracing() {
        return new ArrayList<>(tracing);
    }

    public static void setContext(String context) {
        TestTracing.context.set(context);
    }

    public static void clearContext() {
        TestTracing.context.remove();
    }

    public void resetLastContext(String context) {
        int index = tracing.size() - 1;
        Trace trace = tracing.get(index);
        tracing.set(index, new Trace(trace.status, context, trace.duration, trace.e));
    }

    @Override
    public void startProc() {
        tracing.add(new Trace("start", context.get(), null, null));
    }

    @Override
    public void endProcSuccess(Duration duration) {
        tracing.add(new Trace("success", context.get(), duration, null));
        clearContext();
    }

    @Override
    public void endProcFail(Throwable e, Duration duration) {
        tracing.add(new Trace("fail", context.get(), duration, e));
        clearContext();
    }
}
