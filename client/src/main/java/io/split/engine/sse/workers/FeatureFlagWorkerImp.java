package io.split.engine.sse.workers;

import io.split.client.dtos.Split;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.utils.FeatureFlagsToUpdate;
import io.split.engine.common.Synchronizer;
import io.split.engine.experiments.SplitParser;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.enums.UpdatesFromSSEEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.client.utils.FeatureFlagProcessor.processFeatureFlagChanges;

public class FeatureFlagWorkerImp extends Worker<FeatureFlagChangeNotification> implements FeatureFlagsWorker {
    private static final Logger _log = LoggerFactory.getLogger(FeatureFlagWorkerImp.class);
    private final Synchronizer _synchronizer;
    private final SplitParser _splitParser;
    private final SplitCacheProducer _splitCacheProducer;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final FlagSetsFilter _flagSetsFilter;

    public FeatureFlagWorkerImp(Synchronizer synchronizer, SplitParser splitParser, SplitCacheProducer splitCacheProducer,
                                TelemetryRuntimeProducer telemetryRuntimeProducer, FlagSetsFilter flagSetsFilter) {
        super("Feature flags");
        _synchronizer = checkNotNull(synchronizer);
        _splitParser = splitParser;
        _splitCacheProducer = splitCacheProducer;
        _telemetryRuntimeProducer = telemetryRuntimeProducer;
        _flagSetsFilter = flagSetsFilter;
    }

    @Override
    public void kill(SplitKillNotification splitKillNotification) {
        try {
            _synchronizer.localKillSplit(splitKillNotification);
            _log.debug(String.format("Kill feature flag: %s, changeNumber: %s, defaultTreatment: %s", splitKillNotification.getSplitName(),
                    splitKillNotification.getChangeNumber(), splitKillNotification.getDefaultTreatment()));
        } catch (Exception ex) {
            _log.warn(String.format("Exception on FeatureFlagWorker kill: %s", ex.getMessage()));
        }
    }

    @Override
    protected void executeRefresh(FeatureFlagChangeNotification featureFlagChangeNotification) {
        boolean success = addOrUpdateFeatureFlag(featureFlagChangeNotification);

        if (!success)
            _synchronizer.refreshSplits(featureFlagChangeNotification.getChangeNumber());
    }

    private boolean addOrUpdateFeatureFlag(FeatureFlagChangeNotification featureFlagChangeNotification) {
        if (featureFlagChangeNotification.getChangeNumber() <= _splitCacheProducer.getChangeNumber()) {
            return true;
        }
        try {
            if (featureFlagChangeNotification.getFeatureFlagDefinition() != null &&
                    featureFlagChangeNotification.getPreviousChangeNumber() == _splitCacheProducer.getChangeNumber()) {
                Split featureFlag = featureFlagChangeNotification.getFeatureFlagDefinition();
                FeatureFlagsToUpdate featureFlagsToUpdate = processFeatureFlagChanges(_splitParser, Collections.singletonList(featureFlag),
                        _flagSetsFilter);
                _splitCacheProducer.update(featureFlagsToUpdate.getToAdd(), featureFlagsToUpdate.getToRemove(),
                        featureFlagChangeNotification.getChangeNumber());
                Set<String> segments  = featureFlagsToUpdate.getSegments();
                for (String segmentName: segments) {
                    _synchronizer.forceRefreshSegment(segmentName);
                }
                _telemetryRuntimeProducer.recordUpdatesFromSSE(UpdatesFromSSEEnum.SPLITS);
                return true;
            }
        } catch (Exception e) {
            _log.warn("Something went wrong processing a Feature Flag notification", e);
        }
        return false;
    }
}