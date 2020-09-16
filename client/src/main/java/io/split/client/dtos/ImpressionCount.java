package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;
import io.split.client.impressions.ImpressionCounter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ImpressionCount {

    private static final String FIELD_PER_FEATURE_COUNTS = "pf";

    @SerializedName(FIELD_PER_FEATURE_COUNTS)
    public final List<CountPerFeature> perFeature;

    public ImpressionCount(List<CountPerFeature> cs) {
        perFeature = cs;
    }

    public static ImpressionCount fromImpressionCounterData(Map<ImpressionCounter.Key, Integer> raw) {
        return new ImpressionCount(raw.entrySet().stream()
                .map(e -> new CountPerFeature(e.getKey().featureName(), e.getKey().timeFrame(), e.getValue()))
                .collect(Collectors.toList()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(perFeature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImpressionCount c = (ImpressionCount) o;
        return Objects.equals(perFeature, c.perFeature);
    }

    public static class CountPerFeature {

        private static final String FIELD_FEATURE = "f";
        private static final String FIELD_TIMEFRAME = "m";
        private static final String FIELD_COUNT = "rc";

        @SerializedName(FIELD_FEATURE)
        public final String feature;

        @SerializedName(FIELD_TIMEFRAME)
        public final long timeframe;

        @SerializedName(FIELD_COUNT)
        public final int count;

        public CountPerFeature(String f, long t, int c) {
            feature = f;
            timeframe = t;
            count = c;
        }

        @Override
        public int hashCode() {
            return Objects.hash(feature, timeframe, count);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CountPerFeature c = (CountPerFeature) o;
            return Objects.equals(feature, c.feature) && Objects.equals(timeframe, c.timeframe) &&
                    Objects.equals(count, c.count);
        }
    }
}
