package io.split.client.dtos;


import io.split.client.impressions.Impression;

public class KeyImpression {
    public String feature;
    public String keyName;
    public String bucketingKey;
    public String treatment;
    public String label;
    public long time;
    public Long changeNumber; // can be null if there is no changeNumber
    public Long pt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyImpression that = (KeyImpression) o;

        if (time != that.time) return false;
        if (feature != null ? !feature.equals(that.feature) : that.feature != null) return false;
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

    public static KeyImpression fromImpression(Impression i) {
        KeyImpression ki = new KeyImpression();
        ki.feature = i.split();
        ki.keyName = i.key();
        ki.bucketingKey = i.bucketingKey();
        ki.time = i.time();
        ki.changeNumber = i.changeNumber();
        ki.treatment = i.treatment();
        ki.label = i.appliedRule();
        return ki;
    }
}
