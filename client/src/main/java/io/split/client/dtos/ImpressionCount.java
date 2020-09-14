package io.split.client.dtos;

import io.split.client.impressions.ImpressionCounter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImpressionCount {

    public final List<CountPerFeature> counts;

    public ImpressionCount(List<CountPerFeature> cs) {
        counts = cs;
    }

    public static ImpressionCount fromImpressionCounterData(Map<ImpressionCounter.Key, Integer> raw) {
        return new ImpressionCount(raw.entrySet().stream()
                .map(e -> new CountPerFeature(e.getKey().featureName(), e.getKey().timeFrame(), e.getValue()))
                .collect(Collectors.toList()));
    }

    public static class CountPerFeature {
        public final String feature;
        public final long timeframe;
        public final int count;

        public CountPerFeature(String f, long t, int c) {
            feature = f;
            timeframe = t;
            count = c;
        }
    }
}
