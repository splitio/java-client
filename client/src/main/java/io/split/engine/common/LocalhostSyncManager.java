package io.split.engine.common;

import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalhostSyncManager implements SyncManager {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSyncManager.class);
    private final Synchronizer _localhostSynchronizer;
    private final SDKReadinessGates _gates;

    public LocalhostSyncManager(Synchronizer localhostSynchronizer, SDKReadinessGates sdkReadinessGates){
        _localhostSynchronizer = checkNotNull(localhostSynchronizer);
        _gates = sdkReadinessGates;
    }

    @Override
    public void start() {
        if(!_localhostSynchronizer.syncAll()){
            _log.error("Could not synchronize feature flag and segment files");
            return;
        }
        _gates.sdkInternalReady();
        _localhostSynchronizer.startPeriodicFetching();
    }

    @Override
    public void shutdown() throws IOException {
        _localhostSynchronizer.stopPeriodicFetching();
    }
}
