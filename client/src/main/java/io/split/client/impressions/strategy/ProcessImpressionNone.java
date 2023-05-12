package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionCounter;
import io.split.client.impressions.ImpressionsResult;
import io.split.client.impressions.UniqueKeysTracker;

import java.util.ArrayList;
import java.util.List;

public class ProcessImpressionNone implements ProcessImpressionStrategy{

    private final UniqueKeysTracker _uniqueKeysTracker;
    private final ImpressionCounter _impressionCounter;
    private final boolean _listenerEnabled;

    public ProcessImpressionNone(boolean listenerEnabled,UniqueKeysTracker uniqueKeysTracker, ImpressionCounter impressionCounter) {
        _listenerEnabled = listenerEnabled;
        _uniqueKeysTracker = uniqueKeysTracker;
        _impressionCounter = impressionCounter;
    }

    @Override
    public ImpressionsResult process(List<Impression> impressions) {

        for(Impression impression: impressions){
            _impressionCounter.inc(impression.split(), impression.time(), 1);
            _uniqueKeysTracker.track(impression.split(),impression.key());
        }
        List<Impression> impressionForListener =  this._listenerEnabled ? impressions : null;
        return new ImpressionsResult(new ArrayList<>(), impressionForListener);
    }
}
