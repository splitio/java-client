package io.split.client.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class SplitExecutorFactory {

    public static ScheduledExecutorService buildScheduledExecutorService(ThreadFactory threadFactory, String name, Integer size) {
        return  Executors.newScheduledThreadPool(size, buildThreadFactory(threadFactory, name));
    }

    public static ScheduledExecutorService buildSingleThreadScheduledExecutor(ThreadFactory threadFactory, String name){
        return Executors.newSingleThreadScheduledExecutor(buildThreadFactory(threadFactory, name));
    }

    public static ExecutorService buildExecutorService(ThreadFactory threadFactory, String name) {
        return Executors.newSingleThreadExecutor(buildThreadFactory(threadFactory, name));
    }

    private static ThreadFactory buildThreadFactory(ThreadFactory threadFactory, String name) {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(name);
        if (threadFactory != null) {
            threadFactoryBuilder.setThreadFactory(threadFactory);
        }
        return  threadFactoryBuilder.build();
    }
}