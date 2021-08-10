package io.split.engine.matchers.strings;

import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.matchers.Matcher;

import java.util.Map;
import java.util.regex.Pattern;

public class RegularExpressionMatcher implements Matcher {
    private String _stringMatcher;
    private Pattern _pattern;

    public RegularExpressionMatcher(String matcherValue) {
        _stringMatcher = matcherValue;
        _pattern = Pattern.compile(matcherValue);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (matchValue == null) {
            return false;
        }

        if (matchValue instanceof String) {
            java.util.regex.Matcher matcher = _pattern.matcher((String) matchValue);
            return matcher.find();
        }

        return false;
    }

    @Override
    public String toString() {
        return "matches " + _stringMatcher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegularExpressionMatcher that = (RegularExpressionMatcher) o;

        return _stringMatcher != null ? _stringMatcher.equals(that._stringMatcher) : that._stringMatcher == null;
    }

    @Override
    public int hashCode() {
        return _stringMatcher != null ? _stringMatcher.hashCode() : 0;
    }
}
