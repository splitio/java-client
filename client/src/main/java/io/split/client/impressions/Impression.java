package io.split.client.impressions;

import java.util.Map;

/**
 * Created by adilaijaz on 3/23/17.
 */
public class Impression {
    
    private final String _key;
    private final String _bucketingKey;
    private final String _split;
    private final String _treatment;
    private final long _time;
    private final String _appliedRule;
    private final Long _changeNumber;
    private final Map<String, Object> _attributes;


    public Impression(String key, String bucketingKey, String split, String treatment, long time, String appliedRule, Long changeNumber, Map<String, Object> atributes) {
        _key = key;
        _bucketingKey = bucketingKey;
        _split = split;
        _treatment = treatment;
        _time = time;
        _appliedRule = appliedRule;
        _changeNumber = changeNumber;
        _attributes = atributes;
    }

    public String key() {
        return _key;
    }

    public String bucketingKey() {
        return _bucketingKey;
    }

    public String split() {
        return _split;
    }

    public String treatment() {
        return _treatment;
    }

    public long time() {
        return _time;
    }

    public String appliedRule() {
        return _appliedRule;
    }

    public Long changeNumber() {
        return _changeNumber;
    }

    public Map<String, Object> attributes() {
        return _attributes;
    }
}
