package io.split.engine.matchers;

import io.split.engine.evaluator.EvaluationContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Supports the logic: if user is in split "feature" treatments ["on","off"]
 */
public class DependencyMatcher implements Matcher {
    private String _featureFlag;
    private List<String> _treatments;

    public DependencyMatcher(String featureFlag, List<String> treatments) {
        _featureFlag = featureFlag;
        _treatments = treatments;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (matchValue == null) {
            return false;
        }

        if (!(matchValue instanceof String)) {
            return false;
        }

        String result = evaluationContext.getEvaluator().evaluateFeature((String) matchValue, bucketingKey, _featureFlag, attributes).treatment;

        return _treatments.contains(result);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("in split \"");
        bldr.append(this._featureFlag);
        bldr.append("\" treatment ");
        bldr.append(this._treatments);
        return bldr.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyMatcher that = (DependencyMatcher) o;

        if (!Objects.equals(_featureFlag, that._featureFlag)) return false;
        return Objects.equals(_treatments, that._treatments);
    }

    @Override
    public int hashCode() {
        int result = _featureFlag != null ? _featureFlag.hashCode() : 0;
        result = 31 * result + (_treatments != null ? _treatments.hashCode() : 0);
        return result;
    }
}
