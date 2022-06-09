package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionObserver;

import java.util.ArrayList;
import java.util.List;

public class ProcessImpressionDebug implements ProcessImpressionStrategy{

    private final ImpressionObserver _impressionObserver;

    public ProcessImpressionDebug(ImpressionObserver impressionObserver) {
        _impressionObserver = impressionObserver;
    }

    @Override
    public List<Impression> processImpressions(List<Impression> impressions) {
        List<Impression> impressionsToQueue = new ArrayList<>();
        for(Impression impression : impressions) {
            impression = impression.withPreviousTime(_impressionObserver.testAndSet(impression));
            impressionsToQueue.add(impression);
        }
        return impressionsToQueue;
    }
}
