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

import io.soabase.stages.StagedFuture;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Mechanism for tracing tasks as they execute in the StagedFuture
 */
public interface Tracing {
    /**
     * Returns a new tracer that uses SLF4J and logs as <code>trace()</code>
     *
     * @param logger SLF4J logging facade
     * @return tracer
     */
    static Tracing trace(Logger logger) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.TRACE, Tracing::getContext);
    }

    /**
     * Returns a new tracer that uses SLF4J and logs as <code>trace()</code>
     *
     * @param logger SLF4J logging facade
     * @param maxContext max stack trace entries
     * @return tracer
     */
    static Tracing trace(Logger logger, int maxContext) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.TRACE, () -> getContext(maxContext));
    }

    /**
     * Returns a new tracer that uses SLF4J and logs as <code>debug()</code>
     *
     * @param logger SLF4J logging facade
     * @return tracer
     */
    static Tracing debug(Logger logger) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.DEBUG, Tracing::getContext);
    }

    /**
     * Returns a new tracer that uses SLF4J and logs as <code>debug()</code>
     *
     * @param logger SLF4J logging facade
     * @param maxContext max stack trace entries
     * @return tracer
     */
    static Tracing debug(Logger logger, int maxContext) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.DEBUG, () -> getContext(maxContext));
    }

    /**
     * Returns a new tracer that uses SLF4J and logs as <code>info()</code>
     *
     * @param logger SLF4J logging facade
     * @return tracer
     */
    static Tracing info(Logger logger) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.INFO, Tracing::getContext);
    }

    /**
     * Returns a new tracer that uses SLF4J and logs as <code>info()</code>
     *
     * @param logger SLF4J logging facade
     * @param maxContext max stack trace entries
     * @return tracer
     */
    static Tracing info(Logger logger, int maxContext) {
        return new Slf4JTracing(logger, Slf4JTracing.Level.INFO, () -> getContext(maxContext));
    }

    /**
     * Returns a new tracer that outputs to {@link System#out}
     *
     * @return tracer
     */
    static Tracing console() {
        return new ConsoleTracing(Tracing::getContext);
    }

    /**
     * Returns a new tracer that outputs to {@link System#out}
     *
     * @param maxContext max stack trace entries
     * @return tracer
     */
    static Tracing console(int maxContext) {
        return new ConsoleTracing(() -> getContext(maxContext));
    }

    /**
     * Called just before the task is executed
     */
    void startProc();

    /**
     * Called when the task completes successfully
     *
     * @param duration elapsed time of the task
     */
    void endProcSuccess(Duration duration);

    /**
     * Called when the task throws an exception
     *
     * @param e the exception
     * @param duration elapsed time until the exception
     */
    void endProcFail(Throwable e, Duration duration);

    /**
     * Return execution context information including the current thread and a stack trace
     *
     * @return context
     */
    static List<String> getContext() {
        return getContext(Integer.MAX_VALUE);
    }

    /**
     * Return execution context information including the current thread and a stack trace
     *
     * @param max max stack trace entries
     * @return context
     */
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

    /**
     * Format the log message for task start using the given supplier of context
     *
     * @param contextProc context supplier
     * @return log message
     */
    static String formatStart(Supplier<List<String>> contextProc) {
        return String.format("Start (%s)", contextProc.get());
    }

    /**
     * Format the log message for task success using the given supplier of context and duration
     *
     * @param contextProc context supplier
     * @param duration duration
     * @return log message
     */
    static String formatSuccess(Supplier<List<String>> contextProc, Duration duration) {
        return String.format("Success (%s) - (%d) nanos", contextProc.get(), duration.toNanos());
    }

    /**
     * Format the log message for task failure using the given supplier of context and duration
     *
     * @param contextProc context supplier
     * @param e the failure exception
     * @param duration duration
     * @return log message
     */
    static String formatFail(Supplier<List<String>> contextProc, Throwable e, Duration duration) {
        String error = String.format("type (%s) - message (%s)", e.getClass().getSimpleName(), e.getMessage());
        return String.format("Failure (%s) - error: (%s) - (%d) nanos", contextProc.get(), error, duration.toNanos());
    }
}
