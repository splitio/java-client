package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.UniqueKeysTracker;

import java.util.List;

public class ProcessImpressionMTK implements ProcessImpressionStrategy{

    private final UniqueKeysTracker _uniqueKeysTracker;

    public ProcessImpressionMTK(UniqueKeysTracker uniqueKeysTracker) {
        _uniqueKeysTracker = uniqueKeysTracker;
    }

    @Override
    public List<Impression> processImpressions(List<Impression> impressions) {

        for(Impression impression: impressions){
            _uniqueKeysTracker.track(impression.split(),impression.key());
        }
        return impressions;
    }
}
