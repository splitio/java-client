package io.split.engine.sse.workers;

import io.split.engine.common.Synchronizer;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.SplitKillNotification;

import static com.google.common.base.Preconditions.checkNotNull;

public class FeatureFlagWorkerImp extends Worker<FeatureFlagChangeNotification> implements FeatureFlagsWorker {
    private final Synchronizer _synchronizer;

    public FeatureFlagWorkerImp(Synchronizer synchronizer) {
        super("Feature flags");
        _synchronizer = checkNotNull(synchronizer);
    }

    @Override
    public void kill(SplitKillNotification splitKillNotification) {
        try {
            _synchronizer.localKillSplit(splitKillNotification);
            _log.debug(String.format("Kill feature flag: %s, changeNumber: %s, defaultTreatment: %s", splitKillNotification.getSplitName(), splitKillNotification.getChangeNumber(),
                    splitKillNotification.getDefaultTreatment()));
        } catch (Exception ex) {
            _log.warn(String.format("Exception on FeatureFlagWorker kill: %s", ex.getMessage()));
        }
    }

    @Override
    protected void executeRefresh(FeatureFlagChangeNotification featureFlagChangeNotification) {
        _synchronizer.refreshSplits(featureFlagChangeNotification);
    }
}
