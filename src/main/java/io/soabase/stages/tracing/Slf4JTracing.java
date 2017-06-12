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

import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class Slf4JTracing implements Tracing {
    private final Logger logger;
    private final Level level;
    private final Supplier<List<String>> contextProc;

    public enum Level {
        TRACE() {
            @Override
            public void log(Logger logger, String str) {
                logger.trace(str);
            }

            @Override
            public boolean enabled(Logger logger) {
                return logger.isTraceEnabled();
            }
        },

        DEBUG() {
            @Override
            public void log(Logger logger, String str) {
                logger.debug(str);
            }

            @Override
            public boolean enabled(Logger logger) {
                return logger.isDebugEnabled();
            }
        },

        INFO() {
            @Override
            public void log(Logger logger, String str) {
                logger.info(str);
            }

            @Override
            public boolean enabled(Logger logger) {
                return logger.isInfoEnabled();
            }
        }

        ;

        public abstract void log(Logger logger, String str);

        public abstract boolean enabled(Logger logger);
    }

    public Slf4JTracing(Logger logger, Level level, Supplier<List<String>> contextProc) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.level = Objects.requireNonNull(level, "level cannot be null");
        this.contextProc = Objects.requireNonNull(contextProc, "contextProc cannot be null");
    }

    @Override
    public void startProc() {
        if ( level.enabled(logger) ) {
            level.log(logger, Tracing.formatStart(contextProc));
        }
    }

    @Override
    public void endProcSuccess(Duration duration) {
        if ( level.enabled(logger) ) {
            level.log(logger, Tracing.formatSuccess(contextProc, duration));
        }
    }

    @Override
    public void endProcFail(Throwable e, Duration duration) {
        if ( level.enabled(logger) ) {
            level.log(logger, Tracing.formatFail(contextProc, e, duration));
        }
    }
}
