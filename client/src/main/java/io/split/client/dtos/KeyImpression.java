package io.split.client.dtos;


import com.google.gson.annotations.SerializedName;
import io.split.client.impressions.Impression;

import java.util.Objects;

public class KeyImpression {

    /* package private */ static final String FIELD_KEY_NAME = "k";
    /* package private */ static final String FIELD_BUCKETING_KEY = "b";
    /* package private */ static final String FIELD_TREATMENT = "t";
    /* package private */ static final String FIELD_LABEL = "r";
    /* package private */ static final String FIELD_TIME = "m";
    /* package private */ static final String FIELD_CHANGE_NUMBER = "c";
    /* package private */ static final String FIELD_PREVIOUS_TIME = "pt";
    /* package private */ static final String FIELD_PROPERTIES = "properties";

    public static int MAX_PROPERTIES_LENGTH_BYTES = 32 * 1024;

    public transient String feature; // Non-serializable

    @SerializedName(FIELD_KEY_NAME)
    public String keyName;

    @SerializedName(FIELD_BUCKETING_KEY)
    public String bucketingKey;

    @SerializedName(FIELD_TREATMENT)
    public String treatment;

    @SerializedName(FIELD_LABEL)
    public String label;

    @SerializedName(FIELD_TIME)
    public long time;

    @SerializedName(FIELD_CHANGE_NUMBER)
    public Long changeNumber; // can be null if there is no changeNumber

    @SerializedName(FIELD_PREVIOUS_TIME)
    public Long previousTime;

    @SerializedName(FIELD_PROPERTIES)
    public String properties;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyImpression that = (KeyImpression) o;

        if (time != that.time) return false;
        if (!Objects.equals(feature, that.feature)) return false;
        if (!keyName.equals(that.keyName)) return false;
        if (!treatment.equals(that.treatment)) return false;
        if (properties != null && !properties.equals(that.properties)) return false;

        if (bucketingKey == null) {
            return that.bucketingKey == null;
        }

        return bucketingKey.equals(that.bucketingKey);
    }

    @Override
    public int hashCode() {
        int result = feature != null ? feature.hashCode() : 0;
        result = 31 * result + keyName.hashCode();
        result = 31 * result + (bucketingKey == null ? 0 : bucketingKey.hashCode());
        result = 31 * result + treatment.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }

    public static KeyImpression fromImpression(Impression i) {
        KeyImpression ki = new KeyImpression();
        ki.feature = i.split();
        ki.keyName = i.key();
        ki.bucketingKey = i.bucketingKey();
        ki.time = i.time();
        ki.changeNumber = i.changeNumber();
        ki.treatment = i.treatment();
        ki.label = i.appliedRule();
        ki.previousTime = i.pt();
        ki.properties = i.properties();
        return ki;
    }
}
