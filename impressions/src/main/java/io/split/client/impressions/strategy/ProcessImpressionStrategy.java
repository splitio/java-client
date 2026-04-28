package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionsResult;

import java.util.List;

public interface ProcessImpressionStrategy {

    ImpressionsResult process(List<Impression> impressions);
}
