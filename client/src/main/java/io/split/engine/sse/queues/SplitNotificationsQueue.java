package io.split.engine.sse.queues;

import java.util.concurrent.LinkedBlockingQueue;

public class SplitNotificationsQueue {
    public static LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<>();
}
