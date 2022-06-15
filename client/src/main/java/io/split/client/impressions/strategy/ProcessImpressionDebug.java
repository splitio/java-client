package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionObserver;
import io.split.client.impressions.ImpressionsResult;

import java.util.ArrayList;
import java.util.List;

public class ProcessImpressionDebug implements ProcessImpressionStrategy{

    private final ImpressionObserver _impressionObserver;
    private final boolean _listenerEnabled;

    public ProcessImpressionDebug(boolean listenerEnabled, ImpressionObserver impressionObserver) {
        _listenerEnabled = listenerEnabled;
        _impressionObserver = impressionObserver;
    }

    @Override
    public ImpressionsResult process(List<Impression> impressions) {
        List<Impression> impressionsToQueue = new ArrayList<>();
        for(Impression impression : impressions) {
            impression = impression.withPreviousTime(_impressionObserver.testAndSet(impression));
            impressionsToQueue.add(impression);
        }
        List<Impression> impressionForListener =  this._listenerEnabled ? impressionsToQueue : null;
        return new ImpressionsResult(impressionsToQueue, impressionForListener);
    }
}
