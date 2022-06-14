package io.split.client.impressions.strategy;

import io.split.client.impressions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProcessImpressionOptimized implements ProcessImpressionStrategy{

    private final ImpressionObserver _impressionObserver;
    private final ImpressionCounter _impressionCounter;


    public ProcessImpressionOptimized(ImpressionObserver impressionObserver, ImpressionCounter impressionCounter) {
        _impressionObserver = impressionObserver;
        _impressionCounter = impressionCounter;
    }

    @Override
    public ImpressionsResult process(List<Impression> impressions) {
        List<Impression> impressionsToQueue = new ArrayList<>();
        for(Impression impression : impressions) {
            impression = impression.withPreviousTime(_impressionObserver.testAndSet(impression));
            _impressionCounter.inc(impression.split(), impression.time(), 1);
            if(shouldntQueueImpression(impression)) {
                continue;
            }
            impressionsToQueue.add(impression);
        }
        return new ImpressionsResult(impressions, impressionsToQueue);
    }

    private boolean shouldntQueueImpression(Impression i) {
        return !Objects.isNull(i.pt()) &&
                ImpressionUtils.truncateTimeframe(i.pt()) == ImpressionUtils.truncateTimeframe(i.time());
    }
}
