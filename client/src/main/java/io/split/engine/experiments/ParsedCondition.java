package io.split.engine.experiments;

import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Partition;
import io.split.engine.matchers.CombiningMatcher;

import java.util.List;

/**
 * A pair of matcher and partitions.
 *
 * @author adil
 */
public final class ParsedCondition {

    private final ConditionType _conditionType;
    private final CombiningMatcher _matcher;
    private final List<Partition> _partitions;
    private final String _label;

    public static ParsedCondition createParsedConditionForTests(CombiningMatcher matcher, List<Partition> partitions) {
        return new ParsedCondition(ConditionType.ROLLOUT, matcher, partitions, null);
    }


    public ParsedCondition(ConditionType conditionType, CombiningMatcher matcher, List<Partition> partitions, String label) {
        _conditionType = conditionType;
        _matcher = matcher;
        _partitions = partitions;
        _label = label;
    }


    public ConditionType conditionType() {
        return _conditionType;
    }

    public CombiningMatcher matcher() {
        return _matcher;
    }

    public List<Partition> partitions() {
        return _partitions;
    }

    public String label() {
        return _label;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _matcher.hashCode();

        int partitionsHashCode = 17;
        for (Partition p : _partitions) {
            partitionsHashCode = 31 * partitionsHashCode + p.treatment.hashCode();
            partitionsHashCode = 31 * partitionsHashCode + p.size;
        }

        result = 31 * result + partitionsHashCode;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof ParsedCondition)) return false;

        ParsedCondition other = (ParsedCondition) obj;

        boolean result = _matcher.equals(other._matcher);

        if (!result) {
            return result;
        }

        if (_partitions.size() != other._partitions.size()) {
            return result;
        }

        for (int i = 0; i < _partitions.size(); i++) {
            Partition first = _partitions.get(i);
            Partition second = other._partitions.get(i);

            result &= (first.size == second.size && first.treatment.equals(second.treatment));
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();

        bldr.append(_matcher);
        bldr.append(" then split ");
        boolean first = true;
        for (Partition partition : _partitions) {
            if (!first) {
                bldr.append(',');
            }
            bldr.append(partition.size);
            bldr.append(':');
            bldr.append(partition.treatment);
            first = false;
        }

        return bldr.toString();
    }


}
