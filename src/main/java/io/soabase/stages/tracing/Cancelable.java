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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * {@link java.util.concurrent.CompletableFuture#cancel(boolean)}
 * surprises many users by ignoring the <code>mayInterruptIfRunning</code> argument. i.e. it's not possible
 * to interrupt a chain of CompletableFuture/CompletableStage.
 * </p>
 *
 * <p>
 * Cancelable keeps track of the threads in use by the StagedFuture it is associated with. At
 * any time you can call {@link #cancelChain(boolean)} to interrupt currently running tasks
 * and prevent new tasks from running.
 * </p>
 */
public class Cancelable implements Tracing {
    private final Tracing next;
    private volatile boolean isCanceled = false;
    private final Set<Thread> active = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public Cancelable() {
        this(null);
    }

    /**
     * Adds cancel support and forwards to the given tracer
     *
     * @param next next tracer to call
     */
    public Cancelable(Tracing next) {
        this.next = next;
    }

    /**
     * Cancel the staged future chain. New tasks will throw {@link CancellationException}.
     *
     * @param mayInterruptIfRunning if true, any active threads are interrupted
     */
    public void cancelChain(boolean mayInterruptIfRunning) {
        isCanceled = true;

        if ( mayInterruptIfRunning ) {
            active.forEach(Thread::interrupt);
        }
    }

    @Override
    public void startProc() {
        if (isCanceled) {
            throw new CancellationException("Chain has been canceled");
        }

        active.add(Thread.currentThread());

        if ( next != null ) {
            next.startProc();
        }
    }

    @Override
    public void endProcSuccess(Duration duration) {
        end();

        if ( next != null ) {
            next.endProcSuccess(duration);
        }
    }

    @Override
    public void endProcFail(Throwable e, Duration duration) {
        end();

        if ( next != null ) {
            next.endProcFail(e, duration);
        }
    }

    protected void handleInterrupted() {
        isCanceled = true;
    }

    private void end() {
        Thread currentThread = Thread.currentThread();
        active.remove(currentThread);
        if ( currentThread.isInterrupted() ) {
            handleInterrupted();
        }
    }
}
