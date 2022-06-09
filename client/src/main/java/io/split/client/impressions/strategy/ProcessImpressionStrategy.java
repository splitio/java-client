package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;

import java.util.List;

public interface ProcessImpressionStrategy {

    List<Impression> processImpressions(List<Impression> impressions);
}
