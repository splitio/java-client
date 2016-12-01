package io.split.engine.matchers;

import io.split.client.dtos.DataType;

import static io.split.engine.matchers.Transformers.asDate;
import static io.split.engine.matchers.Transformers.asLong;

/**
 * Created by adilaijaz on 3/7/16.
 */
public class EqualToMatcher implements Matcher {

    private final long _compareTo;
    private final long _normalizedCompareTo;
    private final DataType _dataType;

    public EqualToMatcher(long compareTo, DataType dataType) {
        _compareTo = compareTo;
        _dataType = dataType;

        if (_dataType == DataType.DATETIME) {
            _normalizedCompareTo = asDate(_compareTo);
        } else {
            _normalizedCompareTo = _compareTo;
        }
    }

    @Override
    public boolean match(Object key) {
        Long keyAsLong;

        if (_dataType == DataType.DATETIME) {
            keyAsLong = asDate(key);
        } else {
            keyAsLong = asLong(key);
        }

        if (keyAsLong == null) {
            return false;
        }

        return keyAsLong.longValue() == _normalizedCompareTo;
    }


    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("== ");
        bldr.append(_compareTo);
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (int)(_compareTo ^ (_compareTo >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof EqualToMatcher)) return false;

        EqualToMatcher other = (EqualToMatcher) obj;

        return _compareTo == other._compareTo;
    }

}
