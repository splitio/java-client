package io.split.engine.matchers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.split.client.dtos.MatcherCombiner;
import io.split.engine.evaluator.EvaluationContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Combines the results of multiple matchers using the logical OR or AND.
 *
 * @author adil
 */
public class CombiningMatcher {

    private final ImmutableList<AttributeMatcher> _delegates;
    private final MatcherCombiner _combiner;

    public static CombiningMatcher of(Matcher matcher) {
        return new CombiningMatcher(MatcherCombiner.AND,
                Lists.newArrayList(AttributeMatcher.vanilla(matcher)));
    }

    public static CombiningMatcher of(String attribute, Matcher matcher) {
        return new CombiningMatcher(MatcherCombiner.AND,
                Lists.newArrayList(new AttributeMatcher(attribute, matcher, false)));
    }

    public CombiningMatcher(MatcherCombiner combiner, List<AttributeMatcher> delegates) {
        _delegates = ImmutableList.copyOf(delegates);
        _combiner = combiner;

        checkArgument(_delegates.size() > 0);
    }

    public boolean match(String key, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        if (_delegates.isEmpty()) {
            return false;
        }

        switch (_combiner) {
            case AND:
                return and(key, bucketingKey, attributes, evaluationContext);
            default:
                throw new IllegalArgumentException("Unknown combiner: " + _combiner);
        }

    }

    private boolean and(String key, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext) {
        boolean result = true;
        for (AttributeMatcher delegate : _delegates) {
            result &= (delegate.match(key, bucketingKey, attributes, evaluationContext));
        }
        return result;
    }

    public ImmutableList<AttributeMatcher> attributeMatchers() {
        return _delegates;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("if");
        boolean first = true;
        for (AttributeMatcher matcher : _delegates) {
            if (!first) {
                bldr.append(" " + _combiner);
            }
            bldr.append(" ");
            bldr.append(matcher);
            first = false;
        }
        return bldr.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(_combiner, _delegates);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof CombiningMatcher)) return false;

        CombiningMatcher other = (CombiningMatcher) obj;

        return _combiner.equals(other._combiner) && _delegates.equals(other._delegates);
    }
}
