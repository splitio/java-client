package io.split.engine.sse.workers;

import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.Split;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.utils.FeatureFlagsToUpdate;
import io.split.client.utils.RuleBasedSegmentsToUpdate;
import io.split.engine.common.Synchronizer;
import io.split.engine.experiments.RuleBasedSegmentParser;
import io.split.engine.experiments.SplitParser;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.RuleBasedSegmentChangeNotification;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.enums.UpdatesFromSSEEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.client.utils.FeatureFlagProcessor.processFeatureFlagChanges;
import static io.split.client.utils.RuleBasedSegmentProcessor.processRuleBasedSegmentChanges;

public class FeatureFlagWorkerImp extends Worker<IncomingNotification> implements FeatureFlagsWorker {
    private static final Logger _log = LoggerFactory.getLogger(FeatureFlagWorkerImp.class);
    private final Synchronizer _synchronizer;
    private final SplitParser _splitParser;
    private final RuleBasedSegmentParser _ruleBasedSegmentParser;
    private final SplitCacheProducer _splitCacheProducer;
    private final RuleBasedSegmentCache _ruleBasedSegmentCache;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final FlagSetsFilter _flagSetsFilter;

    public FeatureFlagWorkerImp(Synchronizer synchronizer, SplitParser splitParser, RuleBasedSegmentParser ruleBasedSegmentParser,
                                SplitCacheProducer splitCacheProducer,
                                RuleBasedSegmentCache ruleBasedSegmentCache,
                                TelemetryRuntimeProducer telemetryRuntimeProducer, FlagSetsFilter flagSetsFilter) {
        super("Feature flags");
        _synchronizer = checkNotNull(synchronizer);
        _splitParser = splitParser;
        _ruleBasedSegmentParser = ruleBasedSegmentParser;
        _splitCacheProducer = splitCacheProducer;
        _telemetryRuntimeProducer = telemetryRuntimeProducer;
        _flagSetsFilter = flagSetsFilter;
        _ruleBasedSegmentCache = ruleBasedSegmentCache;
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
    protected void executeRefresh(IncomingNotification incomingNotification) {
        boolean success;
        long changeNumber = 0L;
        long changeNumberRBS = 0L;
        if (incomingNotification.getType() == IncomingNotification.Type.SPLIT_UPDATE) {
            FeatureFlagChangeNotification featureFlagChangeNotification = (FeatureFlagChangeNotification) incomingNotification;
            success = addOrUpdateFeatureFlag(featureFlagChangeNotification);
            changeNumber = featureFlagChangeNotification.getChangeNumber();
        } else {
            RuleBasedSegmentChangeNotification ruleBasedSegmentChangeNotification = (RuleBasedSegmentChangeNotification) incomingNotification;
            success = AddOrUpdateRuleBasedSegment((RuleBasedSegmentChangeNotification) incomingNotification);
            changeNumberRBS = ruleBasedSegmentChangeNotification.getChangeNumber();
        }
        if (!success)
            _synchronizer.refreshSplits(changeNumber, changeNumberRBS);
    }

    private boolean AddOrUpdateRuleBasedSegment(RuleBasedSegmentChangeNotification ruleBasedSegmentChangeNotification) {
        if (ruleBasedSegmentChangeNotification.getChangeNumber() <= _ruleBasedSegmentCache.getChangeNumber()) {
            return true;
        }
        try {
            if (ruleBasedSegmentChangeNotification.getRuleBasedSegmentDefinition() != null &&
                    ruleBasedSegmentChangeNotification.getPreviousChangeNumber() == _ruleBasedSegmentCache.getChangeNumber()) {
                RuleBasedSegment ruleBasedSegment = ruleBasedSegmentChangeNotification.getRuleBasedSegmentDefinition();
                RuleBasedSegmentsToUpdate ruleBasedSegmentsToUpdate = processRuleBasedSegmentChanges(_ruleBasedSegmentParser,
                        Collections.singletonList(ruleBasedSegment));
                _ruleBasedSegmentCache.update(ruleBasedSegmentsToUpdate.getToAdd(), ruleBasedSegmentsToUpdate.getToRemove(),
                        ruleBasedSegmentChangeNotification.getChangeNumber());
                Set<String> segments  = ruleBasedSegmentsToUpdate.getSegments();
                for (String segmentName: segments) {
                    _synchronizer.forceRefreshSegment(segmentName);
                }
                // TODO: Add Telemetry once it is spec'd
//                _telemetryRuntimeProducer.recordUpdatesFromSSE(UpdatesFromSSEEnum.RULE_BASED_SEGMENTS);
                return true;
            }
        } catch (Exception e) {
            _log.warn("Something went wrong processing a Rule based Segment notification", e);
        }
        return false;
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
                if (featureFlagsToUpdate.getToAdd().stream().count() > 0) {
                    Set<String> ruleBasedSegments = featureFlagsToUpdate.getToAdd().get(0).getRuleBasedSegmentsNames();
                    if (!ruleBasedSegments.isEmpty() && !_ruleBasedSegmentCache.contains(ruleBasedSegments)) {
                        return false;
                    }
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