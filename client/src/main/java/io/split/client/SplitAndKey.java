package io.split.client;

/**
 * A split and a key. Key can be null.
 *
 * Primary use case is reading the localhost file and populating a map where the key
 * is SplitAndKey and the value is a treatment.
 *
 * @author adil
 */
public final class SplitAndKey {

    private final String _split;
    private final String _key; // can be null

    public static SplitAndKey of(String split) {
        return new SplitAndKey(split, null);
    }

    public static SplitAndKey of(String split, String key) {
        return new SplitAndKey(split, key);
    }

    public SplitAndKey(String split, String key) {
        _split = split;
        _key = key;
        if (_split == null) {
            throw new IllegalArgumentException("split cannot be null");
        }
    }

    public String split() {
        return _split;
    }

    public String key() {
        return _key;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof SplitAndKey)) {
            return false;
        }

        SplitAndKey other = (SplitAndKey) o;

        return _split.equals(other._split)
                && ((_key == null) ? other._key == null : _key.equals(other._key));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _split.hashCode();
        result = 31 * result + (_key == null ? 0 : _key.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("split: ");
        bldr.append(_split);
        bldr.append(", key: ");
        bldr.append(_key);
        return bldr.toString();
    }
}
