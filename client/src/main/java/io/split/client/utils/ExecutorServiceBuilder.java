package io.split.client.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.SplitClientConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.*;

public class ExecutorServiceBuilder {

    public static ExecutorService buildExecutorService(SplitClientConfig config, String name) {
        ThreadFactory threadFactory = config.getThreadFactory();
        if (threadFactory != null) {
            return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setThreadFactory(threadFactory)
                    .setNameFormat(name)
                    .setDaemon(true)
                    .build());
        } else {
            return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(name).setDaemon(true).build());
        }
    }
}
