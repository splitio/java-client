package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;

public class ImpressionTestUtils {

    public static KeyImpression keyImpression(String feature, String key, String treatment, long time, Long changeNumber) {
        KeyImpression result = new KeyImpression();
        result.feature = feature;
        result.keyName = key;
        result.treatment = treatment;
        result.time = time;
        result.changeNumber = changeNumber;
        return result;
    }
}
