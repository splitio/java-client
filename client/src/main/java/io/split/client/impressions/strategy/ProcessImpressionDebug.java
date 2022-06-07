package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionCounter;
import io.split.client.impressions.ImpressionObserver;

import java.util.ArrayList;
import java.util.List;

public class ProcessImpressionDebug implements ProcessImpressionStrategy{

    @Override
    public List<Impression> processImpressions(List<Impression> impressions, ImpressionObserver impressionObserver, ImpressionCounter impressionCounter, boolean addPreviousTimeEnabled) {
        if(!addPreviousTimeEnabled) { //Only STANDALONE Mode needs to iterate over impressions to add previous time.
            return impressions;
        }

        List<Impression> impressionsToQueue = new ArrayList<>();
        for(Impression impression : impressions) {
            impression = impression.withPreviousTime(impressionObserver.testAndSet(impression));
            impressionsToQueue.add(impression);
        }
        return impressionsToQueue;
    }
}
