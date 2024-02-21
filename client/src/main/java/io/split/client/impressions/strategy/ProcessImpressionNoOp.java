package io.split.client.impressions.strategy;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionsResult;

import java.util.ArrayList;
import java.util.List;

public class ProcessImpressionNoOp implements ProcessImpressionStrategy {
    @Override
    public ImpressionsResult process(List<Impression> impressions) {
        return new ImpressionsResult(new ArrayList<>(), null);
    }
}
