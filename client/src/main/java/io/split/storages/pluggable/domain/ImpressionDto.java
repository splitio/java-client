package io.split.storages.pluggable.domain;

import com.google.gson.annotations.SerializedName;
import io.split.client.dtos.KeyImpression;

import java.util.Objects;

public class ImpressionDto {

    /* package private */ static final String FIELD_FEATURE = "f";
    /* package private */ static final String FIELD_KEY_NAME = "k";
    /* package private */ static final String FIELD_BUCKETING_KEY = "b";
    /* package private */ static final String FIELD_TREATMENT = "t";
    /* package private */ static final String FIELD_LABEL = "r";
    /* package private */ static final String FIELD_TIME = "m";
    /* package private */ static final String FIELD_CHANGE_NUMBER = "c";

    @SerializedName(FIELD_FEATURE)
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
    public Long changeNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyImpression that = (KeyImpression) o;

        if (time != that.time) return false;
        if (!Objects.equals(feature, that.feature)) return false;
        if (!keyName.equals(that.keyName)) return false;
        if (!treatment.equals(that.treatment)) return false;

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

    public static ImpressionDto fromKeyImpression(KeyImpression i) {
        ImpressionDto iDto = new ImpressionDto();
        iDto.feature = i.feature;
        iDto.keyName = i.keyName;
        iDto.bucketingKey = i.bucketingKey;
        iDto.time = i.time;
        iDto.changeNumber = i.changeNumber;
        iDto.treatment = i.treatment;
        iDto.label = i.label;
        return iDto;
    }

}
