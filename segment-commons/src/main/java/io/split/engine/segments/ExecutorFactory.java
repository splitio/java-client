package io.split.engine.segments;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public interface ExecutorFactory {
    ScheduledExecutorService build(ThreadFactory threadFactory, String nameFormat, int numThreads);
}
