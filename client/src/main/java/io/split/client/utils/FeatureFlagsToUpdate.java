package io.split.client.utils;

import io.split.engine.experiments.ParsedSplit;

import java.util.List;
import java.util.Set;

public class FeatureFlagsToUpdate {
    List<ParsedSplit> toAdd;
    List<String> toRemove;
    Set<String> segments;

    public FeatureFlagsToUpdate(List<ParsedSplit> toAdd, List<String> toRemove, Set<String> segments) {
        this.toAdd = toAdd;
        this.toRemove = toRemove;
        this.segments = segments;
    }

    public List<ParsedSplit> getToAdd() {
        return toAdd;
    }

    public List<String> getToRemove() {
        return toRemove;
    }

    public Set<String> getSegments() {
        return segments;
    }
}