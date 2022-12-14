package io.split.engine.common;

import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.telemetry.synchronizer.TelemetrySyncTask;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTasks {
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final SegmentSynchronizationTask _segmentSynchronizationTask;
    private final ImpressionsManager _impressionManager;
    private final EventsTask _eventsTask;
    private final TelemetrySyncTask _telemetrySyncTask;
    private final UniqueKeysTracker _uniqueKeysTracker;

    private SplitTasks (SplitSynchronizationTask splitSynchronizationTask,
                        SegmentSynchronizationTask segmentSynchronizationTask,
                        ImpressionsManager impressionsManager,
                        EventsTask eventsTask,
                        TelemetrySyncTask telemetrySyncTask,
                        UniqueKeysTracker uniqueKeysTracker){
        _splitSynchronizationTask = splitSynchronizationTask;
        _segmentSynchronizationTask = segmentSynchronizationTask;
        _impressionManager = impressionsManager;
        _eventsTask = eventsTask;
        _uniqueKeysTracker = uniqueKeysTracker;
        _telemetrySyncTask = telemetrySyncTask;
    }

    public static SplitTasks build (SplitSynchronizationTask splitSynchronizationTask,
                                    SegmentSynchronizationTask segmentSynchronizationTask,
                                    ImpressionsManager impressionsManager,
                                    EventsTask eventsTask,
                                    TelemetrySyncTask telemetrySyncTask,
                                    UniqueKeysTracker uniqueKeysTracker) {
        return new SplitTasks ( splitSynchronizationTask,
                                segmentSynchronizationTask,
                                impressionsManager,
                                eventsTask,
                                telemetrySyncTask,
                                uniqueKeysTracker);
    }

    public SplitSynchronizationTask getSplitSynchronizationTask() {
        return _splitSynchronizationTask;
    }

    public SegmentSynchronizationTask getSegmentSynchronizationTask() {
        return _segmentSynchronizationTask;
    }

    public ImpressionsManager getImpressionManager() {
        return _impressionManager;
    }

    public EventsTask getEventsTask() {
        return _eventsTask;
    }

    public TelemetrySyncTask getTelemetrySyncTask() {
        return _telemetrySyncTask;
    }

    public UniqueKeysTracker getUniqueKeysTracker() {
        return _uniqueKeysTracker;
    }
}