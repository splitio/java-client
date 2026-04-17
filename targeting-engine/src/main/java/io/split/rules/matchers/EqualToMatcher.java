package io.split.rules.matchers;

import io.split.rules.model.DataType;
import io.split.rules.engine.EvaluationContext;

import java.util.Map;

import static io.split.rules.matchers.Transformers.asDate;
import static io.split.rules.matchers.Transformers.asLong;

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
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        Long keyAsLong;

        if (_dataType == DataType.DATETIME) {
            keyAsLong = asDate(matchValue);
        } else {
            keyAsLong = asLong(matchValue);
        }

        return keyAsLong != null && keyAsLong == _normalizedCompareTo;
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
