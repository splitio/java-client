package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;
import io.split.client.utils.MurmurHash3;

public class ImpressionHasher {

    private static final String HASHABLE_FORMAT = "%s:%s:%s:%d";
    private static final String UNKNOWN = "UNKNOWN";

    private static String unknownIfNull(String s) {
        return (s == null) ? UNKNOWN : s;
    }

    private static Long zeroIfNull(Long l) {
        return (l == null) ? 0 : l;
    }

    public static Long process(KeyImpression impression) {
        if (null == impression) {
            return null;
        }
        return MurmurHash3.hash128x64(String.format(HASHABLE_FORMAT,
                unknownIfNull(impression.keyName),
                unknownIfNull(impression.feature),
                unknownIfNull(impression.label),
                zeroIfNull(impression.changeNumber)).getBytes())[0];
    }
}
