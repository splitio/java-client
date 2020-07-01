package io.split.engine.sse.workers;

public interface SplitsWorker extends Worker<Long> {
    void killSplit(long changeNumber, String splitName, String defaultTreatment);
}
