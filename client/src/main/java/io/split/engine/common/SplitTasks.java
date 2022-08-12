package io.split.engine.common;

import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.ImpressionsManagerImpl;
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

    private SplitTasks (SplitSynchronizationTask splitSynchronizationTask,
                        SegmentSynchronizationTask segmentSynchronizationTaskImp,
                        ImpressionsManager impressionsManager,
                        EventsTask eventsTask,
                        TelemetrySyncTask telemetrySyncTask){
        _splitSynchronizationTask = splitSynchronizationTask;
        _segmentSynchronizationTask = segmentSynchronizationTaskImp;
        _impressionManager = impressionsManager;
        _eventsTask = eventsTask;
        _telemetrySyncTask = checkNotNull(telemetrySyncTask);
    }

    public static SplitTasks build (SplitSynchronizationTask splitSynchronizationTask,
                                    SegmentSynchronizationTaskImp segmentSynchronizationTaskImp,
                                    ImpressionsManagerImpl impressionsManager,
                                    EventsTask eventsTask,
                                    TelemetrySyncTask telemetrySyncTask) {
        return new SplitTasks ( splitSynchronizationTask,
                                segmentSynchronizationTaskImp,
                                impressionsManager,
                                eventsTask,
                                telemetrySyncTask);
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
}