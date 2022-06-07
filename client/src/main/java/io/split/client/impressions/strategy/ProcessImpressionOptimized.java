package io.split.client.impressions.strategy;

import io.split.client.impressions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProcessImpressionOptimized implements ProcessImpressionStrategy{

    @Override
    public List<Impression> processImpressions(List<Impression> impressions, ImpressionObserver impressionObserver, ImpressionCounter impressionCounter, boolean addPreviousTimeEnabled, UniqueKeysTracker uniqueKeysTracker) {
        List<Impression> impressionsToQueue = new ArrayList<>();
        for(Impression impression : impressions) {
            impression = impression.withPreviousTime(impressionObserver.testAndSet(impression));
            impressionCounter.inc(impression.split(), impression.time(), 1);
            if(!shouldQueueImpression(impression)) {
                continue;
            }
            impressionsToQueue.add(impression);
        }
        return impressionsToQueue;
    }

    private boolean shouldQueueImpression(Impression i) {
        return Objects.isNull(i.pt()) ||
                ImpressionUtils.truncateTimeframe(i.pt()) != ImpressionUtils.truncateTimeframe(i.time());
    }
}
