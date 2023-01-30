package io.split.engine.common;

import java.io.IOException;

public class ConsumerSyncManager implements SyncManager {
    private final Synchronizer _redisSynchronizer;

    public ConsumerSyncManager(Synchronizer redisSynchronizer){
        _redisSynchronizer = redisSynchronizer;
    }

    @Override
    public void start() {
        _redisSynchronizer.startPeriodicDataRecording();
    }

    @Override
    public void shutdown(long splitCount, long segmentCount, long segmentKeyCount) throws IOException {
        _redisSynchronizer.stopPeriodicDataRecording(splitCount, segmentCount, segmentKeyCount);
    }
}
