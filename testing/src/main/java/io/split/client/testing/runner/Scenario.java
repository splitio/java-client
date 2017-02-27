package io.split.client.testing.runner;

import io.split.client.testing.SplitClientForTest;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class Scenario {
    private TreeMap<String, String> _tests;

    public Scenario() {
        _tests = new TreeMap<>();
    }

    public Scenario(Scenario other) {
        _tests = new TreeMap<>(other.tests());
    }

    public SortedMap<String, String> tests() {
        return _tests;
    }

    public void addTest(String feature, String treatment) {
        _tests.put(feature, treatment);
    }

    public void merge(Scenario other) {
        _tests.putAll(other.tests());
    }

    public void apply(SplitClientForTest splitClient) {
        splitClient.registerTreatments(_tests);
    }

    @Override
    public String toString() {
        String output = null;
        for (Map.Entry<String, String> entry : _tests.entrySet()) {
            String test = entry.getKey() + "=" + entry.getValue();
            if (output != null) {
                output += ",";
            }
            output += test;
        }
        return output;
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
