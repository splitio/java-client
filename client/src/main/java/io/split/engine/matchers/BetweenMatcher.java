package io.split.engine.matchers;

import io.split.client.dtos.DataType;
import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.evaluator.Evaluator;

import java.util.Map;

import static io.split.engine.matchers.Transformers.asDateHourMinute;
import static io.split.engine.matchers.Transformers.asLong;

/**
 * Supports the logic: if user.age is between x and y
 *
 * @author adil
 */
public class BetweenMatcher implements Matcher {
    private final long _start;
    private final long _end;
    private final long _normalizedStart;
    private final long _normalizedEnd;

    private final DataType _dataType;

    public BetweenMatcher(long start, long end, DataType dataType) {
        _start = start;
        _end = end;
        _dataType = dataType;

        if (_dataType == DataType.DATETIME) {
            _normalizedStart = asDateHourMinute(_start);
            _normalizedEnd = asDateHourMinute(_end);
        } else {
            _normalizedStart = _start;
            _normalizedEnd = _end;
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

        return keyAsLong >= _normalizedStart && keyAsLong <= _normalizedEnd;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("between ");
        bldr.append(_start);
        bldr.append(" and ");
        bldr.append(_end);
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (int)(_start ^ (_start >>> 32));
        result = 31 * result + (int)(_end ^ (_end >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof BetweenMatcher)) return false;

        BetweenMatcher other = (BetweenMatcher) obj;

        return _start == other._start && _end == other._end;
    }

}
