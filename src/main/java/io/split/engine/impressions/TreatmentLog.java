package io.split.engine.impressions;

/**
 * Created by adilaijaz on 7/2/15.
 */
public interface TreatmentLog {
    /**
     * Logs the fact that a 'key' saw 'treatment' at 'time' for 'name'.
     * Implementations MUST NOT throw any exceptions.
     *  @param key        who
     * @param bucketingKey
     * @param feature   for which feature
     * @param treatment saw what
     * @param time      at what time
     */
    void log(String key, String bucketingKey, String feature, String treatment, long time, String label, Long changeNumber);

    public static final class NoopTreatmentLog implements TreatmentLog {

        @Override
        public void log(String key, String bucketingKey, String feature, String treatment, long time, String label, Long changeNumber) {
            // noop
        }
    }

}
