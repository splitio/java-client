package io.split.client.testing.runner;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.split.client.testing.SplitClientForTest;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class Scenario {
    private TreeMap<String, String> _tests;

    public Scenario() {
        _tests = Maps.newTreeMap();
    }

    public Scenario(Scenario other) {
        Preconditions.checkNotNull(other);

        _tests = Maps.newTreeMap(other.tests());
    }

    public SortedMap<String, String> tests() {
        return _tests;
    }

    public void addTest(String feature, String treatment) {
        Preconditions.checkNotNull(feature);
        Preconditions.checkNotNull(treatment);

        _tests.put(feature, treatment);
    }

    public void merge(Scenario other) {
        Preconditions.checkNotNull(other);

        _tests.putAll(other.tests());
    }

    public void apply(SplitClientForTest splitClient) {
        Preconditions.checkNotNull(splitClient);

        splitClient.registerTreatments(_tests);
    }

    @Override
    public String toString() {
        return Joiner.on(",")
                .withKeyValueSeparator("=")
                .join(_tests);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Scenario scenario = (Scenario) o;
        return Objects.equals(_tests, scenario._tests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_tests);
    }
}
