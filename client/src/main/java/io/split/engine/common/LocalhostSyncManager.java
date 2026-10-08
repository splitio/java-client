package io.split.engine.common;

import io.split.client.api.SdkEventMetadata;
import io.split.client.lifecycle.SdkEventsNotifier;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalhostSyncManager implements SyncManager {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSyncManager.class);
    private final Synchronizer _localhostSynchronizer;
    private final SDKReadinessGates _gates;

    private final SdkEventsNotifier _notifier;

    public LocalhostSyncManager(Synchronizer localhostSynchronizer, SDKReadinessGates sdkReadinessGates){
        this(localhostSynchronizer, sdkReadinessGates, SdkEventsNotifier.NOOP);
    }

    public LocalhostSyncManager(Synchronizer localhostSynchronizer, SDKReadinessGates sdkReadinessGates,
                                SdkEventsNotifier notifier) {
        _localhostSynchronizer = checkNotNull(localhostSynchronizer);
        _gates = sdkReadinessGates;
        _notifier = checkNotNull(notifier);
    }

    @Override
    public void start() {
        if(!_localhostSynchronizer.syncAll()){
            _log.error("Could not synchronize feature flag and segment files");
            return;
        }
        _gates.sdkInternalReady();
        _notifier.notify(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        _localhostSynchronizer.startPeriodicFetching();
    }

    @Override
    public void shutdown() throws IOException {
        _localhostSynchronizer.stopPeriodicFetching();
    }
}
