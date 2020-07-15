package io.split.engine.sse.workers;

public interface SplitsWorker {
    void addToQueue(Long element);
    void start();
    void stop();
    void killSplit(long changeNumber, String splitName, String defaultTreatment);
}
