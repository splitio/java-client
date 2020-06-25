package io.split.engine.sse.workers;

public interface SplitsWorker extends Runnable{
    void addToQueue(long changeNumber);
    void killSplit(long changeNumber, String splitName, String defaultTreatment);
}
