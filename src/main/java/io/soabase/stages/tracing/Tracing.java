package io.soabase.stages.tracing;

import io.soabase.stages.StagedFuture;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public interface Tracing {
    static Tracing trace(Logger logger) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.TRACE, Tracing::getContext);
    }

    static Tracing trace(Logger logger, int maxContext) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.TRACE, () -> getContext(maxContext));
    }

    static Tracing debug(Logger logger) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.DEBUG, Tracing::getContext);
    }

    static Tracing debug(Logger logger, int maxContext) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.DEBUG, () -> getContext(maxContext));
    }

    static Tracing info(Logger logger) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.INFO, Tracing::getContext);
    }

    static Tracing info(Logger logger, int maxContext) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.INFO, () -> getContext(maxContext));
    }

    static Tracing console() {
        return new ConsoleTracing(Tracing::getContext);
    }

    static Tracing console(int maxContext) {
        return new ConsoleTracing(() -> getContext(maxContext));
    }

    void startProc();

    void endProcSuccess(Duration duration);

    void endProcFail(Throwable e, Duration duration);

    static List<String> getContext() {
        return getContext(Integer.MAX_VALUE);
    }

    static List<String> getContext(int max) {
        List<String> context = new ArrayList<>();
        Thread currentThread = Thread.currentThread();
        ThreadGroup threadGroup = currentThread.getThreadGroup();
        context.add("@" + ((threadGroup != null) ? threadGroup.getName() : "-") + ":" + currentThread.getName());
        StackTraceElement[] stackTrace = currentThread.getStackTrace();

        int index = 0;
        boolean started = false;
        while ( ((context.size() - 1) < max) && (index < stackTrace.length) ) { // minus 1 for the thread name
            StackTraceElement trace = stackTrace[index++];
            if ( started ) {
                context.add(trace.getClassName() + ":" + trace.getLineNumber());
            } else {
                started = trace.getClassName().startsWith(StagedFuture.class.getName());
            }
        }
        return context;
    }

    static String formatStart(Supplier<List<String>> contextProc) {
        return String.format("Start (%s)", contextProc.get());
    }

    static String formatSuccess(Supplier<List<String>> contextProc, Duration duration) {
        return String.format("Success (%s) - (%d) nanos", contextProc.get(), duration.toNanos());
    }

    static String formatFail(Supplier<List<String>> contextProc, Throwable e, Duration duration) {
        String error = String.format("type (%s) - message (%s)", e.getClass().getSimpleName(), e.getMessage());
        return String.format("Failure (%s) - error: (%s) - (%d) nanos", contextProc.get(), error, duration.toNanos());
    }
}
