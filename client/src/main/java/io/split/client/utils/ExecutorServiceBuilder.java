package io.split.client.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.SplitClientConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

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

    public static ScheduledExecutorService buildScheduledExecutorService(SplitClientConfig config, String name, Integer size) {
        ThreadFactory threadFactory = config.getThreadFactory();
        if (threadFactory != null) {
            return Executors.newScheduledThreadPool(size, new ThreadFactoryBuilder()
                    .setThreadFactory(threadFactory)
                    .setDaemon(true)
                    .setNameFormat(name)
                    .build());
        } else {
            return Executors.newScheduledThreadPool(size, new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(name)
                    .build());
        }
    }

    public static ScheduledExecutorService buildSingleThreadScheduledExecutor(SplitClientConfig config, String name){
        ThreadFactory threadFactory = config.getThreadFactory();
        if (threadFactory != null) {
            return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                    .setThreadFactory(threadFactory)
                    .setDaemon(true)
                    .setNameFormat(name)
                    .build());
        } else {
            return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(name)
                    .build());
        }
    }
}