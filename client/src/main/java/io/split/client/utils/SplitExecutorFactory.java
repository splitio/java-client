package io.split.client.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class SplitExecutorFactory {

    public static ScheduledExecutorService buildScheduledExecutorService(ThreadFactory threadFactory, String name, Integer size) {
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

    public static ScheduledExecutorService buildSingleThreadScheduledExecutor(ThreadFactory threadFactory, String name){
        return Executors.newSingleThreadScheduledExecutor(buildThreadFactory(threadFactory, name));
    }

    public static ExecutorService buildExecutorService(ThreadFactory threadFactory, String name) {
        return Executors.newSingleThreadExecutor(buildThreadFactory(threadFactory, name));
    }

    private static ThreadFactory buildThreadFactory(ThreadFactory threadFactory, String name) {
        if (threadFactory != null) {
            return new ThreadFactoryBuilder()
                    .setThreadFactory(threadFactory)
                    .setDaemon(true)
                    .setNameFormat(name)
                    .build();
        } else {
            return new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(name)
                    .build();
        }
    }
}