package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionCounter;
import io.split.client.impressions.ImpressionObserver;

import java.util.List;

public class ProcessImpressionMTK implements ProcessImpressionStrategy{

    @Override
    public List<Impression> processImpressions(List<Impression> impressions, ImpressionObserver impressionObserver, ImpressionCounter impressionCounter, boolean addPreviousTimeEnabled) {
        return null;
    }
}
