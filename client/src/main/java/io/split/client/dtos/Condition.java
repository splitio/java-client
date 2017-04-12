package io.split.client.dtos;

import java.util.List;

/**
 * A single condition in the the name. If the condition is fulfilled,
 * the partitions are used.
 *
 * @author adil
 */
public class Condition {
    public ConditionType conditionType;
    public MatcherGroup matcherGroup;
    public List<Partition> partitions;
    public String label;
}
