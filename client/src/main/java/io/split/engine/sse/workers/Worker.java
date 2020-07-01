package io.split.engine.sse.workers;

public interface Worker<T> extends Runnable {
    void addToQueue(T element);
    void start();
    void stop();
}
