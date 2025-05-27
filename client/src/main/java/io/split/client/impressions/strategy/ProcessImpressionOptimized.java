package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionObserver;
import io.split.client.impressions.ImpressionUtils;
import io.split.client.impressions.ImpressionsResult;
import io.split.client.impressions.ImpressionCounter;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProcessImpressionOptimized implements ProcessImpressionStrategy{

    private final ImpressionObserver _impressionObserver;
    private final ImpressionCounter _impressionCounter;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final boolean _listenerEnabled;


    public ProcessImpressionOptimized(boolean listenerEnabled, ImpressionObserver impressionObserver, ImpressionCounter impressionCounter,
                                      TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _telemetryRuntimeProducer = telemetryRuntimeProducer;
        _listenerEnabled = listenerEnabled;
        _impressionObserver = impressionObserver;
        _impressionCounter = impressionCounter;
    }

    @Override
    public ImpressionsResult process(List<Impression> impressions) {
        List<Impression> impressionsToQueue = new ArrayList<>();
        for(Impression impression : impressions) {
            if (impression.properties() == null) {
                impression = impression.withPreviousTime(_impressionObserver.testAndSet(impression));
                if (!Objects.isNull(impression.pt()) && impression.pt() != 0) {
                    _impressionCounter.inc(impression.split(), impression.time(), 1);
                }
                if (shouldntQueueImpression(impression)) {
                    continue;
                }
            }
            impressionsToQueue.add(impression);
        }
        List<Impression> impressionForListener =  this._listenerEnabled ? impressions : null;

        _telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, impressions.size()-
                (long)impressionsToQueue.size());

        return new ImpressionsResult(impressionsToQueue, impressionForListener);
    }

    private boolean shouldntQueueImpression(Impression i) {
        return !Objects.isNull(i.pt()) &&
                ImpressionUtils.truncateTimeframe(i.pt()) == ImpressionUtils.truncateTimeframe(i.time());
    }
}
