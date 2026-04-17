package io.split.rules.matchers;

import io.split.rules.engine.EvaluationContext;

import java.util.Map;

import static io.split.rules.matchers.Transformers.asBoolean;

public class BooleanMatcher implements Matcher {
    private boolean _booleanValue;

    public BooleanMatcher(boolean booleanValue) {
        _booleanValue = booleanValue;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (matchValue == null) {
            return false;
        }

        Boolean valueAsBoolean = asBoolean(matchValue);

        return valueAsBoolean != null && valueAsBoolean == _booleanValue;
    }

    @Override
    public String toString() {
        return "is " + _booleanValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BooleanMatcher that = (BooleanMatcher) o;

        return _booleanValue == that._booleanValue;
    }

    @Override
    public int hashCode() {
        return (_booleanValue ? 1 : 0);
    }
}
