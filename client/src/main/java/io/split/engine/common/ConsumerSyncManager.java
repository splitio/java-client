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
    public void shutdown() throws IOException {
        _redisSynchronizer.stopPeriodicDataRecording();
    }
}
