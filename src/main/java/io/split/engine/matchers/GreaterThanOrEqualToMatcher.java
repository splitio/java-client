package io.split.engine.matchers;

import io.split.client.dtos.DataType;

import static io.split.engine.matchers.Transformers.asDateHourMinute;
import static io.split.engine.matchers.Transformers.asLong;

/**
 * Created by adilaijaz on 3/7/16.
 */
public class GreaterThanOrEqualToMatcher implements Matcher {

    private final long _compareTo;
    private final long _normalizedCompareTo;
    private final DataType _dataType;

    public GreaterThanOrEqualToMatcher(long compareTo, DataType dataType) {
        _compareTo = compareTo;
        _dataType = dataType;

        if (_dataType == DataType.DATETIME) {
            _normalizedCompareTo = asDateHourMinute(_compareTo);
        } else {
            _normalizedCompareTo = _compareTo;
        }
    }

    @Override
    public boolean match(Object key) {
        Long keyAsLong;

        if (_dataType == DataType.DATETIME) {
            keyAsLong = asDateHourMinute(key);
        } else {
            keyAsLong = asLong(key);
        }

        if (keyAsLong == null) {
            return false;
        }

        return keyAsLong.longValue() >= _normalizedCompareTo;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(">= ");
        bldr.append(_compareTo);
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Long.hashCode(_compareTo);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof GreaterThanOrEqualToMatcher)) return false;

        GreaterThanOrEqualToMatcher other = (GreaterThanOrEqualToMatcher) obj;

        return _compareTo == other._compareTo;
    }

}
