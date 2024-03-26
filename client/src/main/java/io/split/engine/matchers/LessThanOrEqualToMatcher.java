package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import io.split.engine.evaluator.EvaluationContext;

import java.util.Map;

import static io.split.engine.matchers.Transformers.asDateHourMinute;
import static io.split.engine.matchers.Transformers.asLong;

/**
 * Created by adilaijaz on 3/7/16.
 */
public class LessThanOrEqualToMatcher implements Matcher {
    private final long _compareTo;
    private final long _normalizedCompareTo;
    private final DataType _dataType;

    public LessThanOrEqualToMatcher(long compareTo, DataType dataType) {
        _compareTo = compareTo;
        _dataType = dataType;

        if (_dataType == DataType.DATETIME) {
            _normalizedCompareTo = asDateHourMinute(_compareTo);
        } else {
            _normalizedCompareTo = _compareTo;
        }
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        Long keyAsLong;

        if (_dataType == DataType.DATETIME) {
            keyAsLong = asDateHourMinute(matchValue);
        } else {
            keyAsLong = asLong(matchValue);
        }

        if (keyAsLong == null) {
            return false;
        }

        return keyAsLong <= _normalizedCompareTo;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("<= ");
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
        if (!(obj instanceof LessThanOrEqualToMatcher)) return false;

        LessThanOrEqualToMatcher other = (LessThanOrEqualToMatcher) obj;

        return _compareTo == other._compareTo;
    }

}
