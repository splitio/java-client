package io.split.client.api;

/**
 * Created by adilaijaz on 10/1/16.
 */
public final class Key {
    private final String _matchingKey;
    private final String _bucketingKey;

    public Key(String matchingKey, String bucketingKey) {
        _matchingKey = matchingKey;
        _bucketingKey = bucketingKey;

        if (_matchingKey == null) {
            throw new IllegalArgumentException("Matching key cannot be null");
        }

        if (_bucketingKey == null) {
            throw new IllegalArgumentException("Bucketing key cannot be null");
        }
    }

    public String matchingKey() {
        return _matchingKey;
    }

    public String bucketingKey() {
        return _bucketingKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (!(o instanceof Key)) {
            return false;
        }

        Key other = (Key) o;
        return _matchingKey.equals(other._matchingKey) &&
                _bucketingKey.equals(other._bucketingKey);
    }

    @Override
    public int hashCode() {
        int result = 17;


        result *= 1000003;
        result ^= _matchingKey.hashCode();

        result *= 1000003;
        result ^= _bucketingKey.hashCode();

        return result;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(_matchingKey);
        bldr.append(", ");
        bldr.append(_bucketingKey);
        return bldr.toString();
    }

}
