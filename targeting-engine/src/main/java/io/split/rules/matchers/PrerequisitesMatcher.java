package io.split.rules.matchers;

import io.split.rules.engine.EvaluationContext;
import io.split.rules.model.Prerequisite;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PrerequisitesMatcher implements Matcher {
    private List<Prerequisite> _prerequisites;

    public PrerequisitesMatcher(List<Prerequisite> prerequisites) {
        _prerequisites = prerequisites;
    }

    public List<Prerequisite> getPrerequisites() { return _prerequisites; }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (matchValue == null) {
            return false;
        }

        if (!(matchValue instanceof String)) {
            return false;
        }

        if (_prerequisites == null) {
            return true;
        }

        for (Prerequisite prerequisites : _prerequisites) {
            String treatment = evaluationContext.evaluate((String) matchValue, bucketingKey,
                    prerequisites.featureFlagName(), attributes).treatment;
            if (!prerequisites.treatments().contains(treatment)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("prerequisites: ");
        if (this._prerequisites != null) {
            bldr.append(this._prerequisites.stream().map(pr -> pr.featureFlagName() + " " +
                    pr.treatments().toString()).map(Object::toString).collect(Collectors.joining(", ")));
        }
        return bldr.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrerequisitesMatcher that = (PrerequisitesMatcher) o;

        return Objects.equals(_prerequisites, that._prerequisites);
    }

    @Override
    public int hashCode() {
        int result = _prerequisites != null ? _prerequisites.hashCode() : 0;
        result = 31 * result + (_prerequisites != null ? _prerequisites.hashCode() : 0);
        return result;
    }
}
