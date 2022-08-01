package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UniqueKeys {

    static final String KEYS = "keys";

    @SerializedName(KEYS)
    public List<UniqueKey> uniqueKeys;

    public UniqueKeys(List<UniqueKey> uniqueKeys) {
        this.uniqueKeys = uniqueKeys;
    }

    public static class UniqueKey {
        static final String FEATURE = "f";

        static final String FEATURE_KEYS = "ks";

        @SerializedName(FEATURE)
        public String featureName;

        @SerializedName(FEATURE_KEYS)
        public List<String> keysDto;

        public UniqueKey(String featureName, List<String> keysDto) {
            this.featureName = featureName;
            this.keysDto = keysDto;
        }
    }
}