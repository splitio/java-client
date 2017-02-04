package io.split.client.testing.runner;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.split.client.testing.annotations.SplitScenario;
import io.split.client.testing.annotations.SplitSuite;
import io.split.client.testing.annotations.SplitTest;
import io.split.client.testing.annotations.SplitTestClient;

import java.util.Set;

public class Suite {
    private Set<Scenario> _scenarios;

    public Suite() {
        // Default to the All Control Scenario
        _scenarios = Sets.newHashSet();
        _scenarios.add(new Scenario());
    }

    public Suite(Suite other) {
        _scenarios = Sets.newHashSet(other._scenarios);
    }

    public Suite(SplitTest splitTest) {
        SplitTest[] splitTests = {splitTest};
        _scenarios = permuteTests(splitTests);
    }

    public Suite(SplitScenario splitScenario) {
        _scenarios = permuteTests(splitScenario.tests());
    }

    public Suite(SplitSuite splitSuite) {
        _scenarios = Sets.newHashSet();
        for (SplitScenario scenario : splitSuite.scenarios()) {
            _scenarios.addAll(permuteTests(scenario.tests()));
        }
    }

    public Suite(SplitTestClient splitTestClient) {
        _scenarios = Sets.newHashSet();
        for (SplitScenario scenario : splitTestClient.scenarios()) {
            _scenarios.addAll(permuteTests(scenario.tests()));
        }
    }

    /**
     * Generate All Permutations of SplitTests
     * <p>
     * For each feature permute all treatments into a set of scenarios
     *
     * @param splitTests The test features and treatments to permute across
     * @return The List of permuted Scenarios
     */
    private static Set<Scenario> permuteTests(SplitTest[] splitTests) {
        Set<Scenario> scenarios = Sets.newHashSet();
        scenarios.add(new Scenario());

        for (SplitTest test : splitTests) {
            Set<Scenario> permutedScenarios = Sets.newHashSet();
            for (String treatment : test.treatments()) {
                for (Scenario scenario : scenarios) {
                    Scenario permutedScenario = new Scenario(scenario);
                    permutedScenario.addTest(test.feature(), treatment);
                    permutedScenarios.add(permutedScenario);
                }
            }
            scenarios = permutedScenarios;
        }

        return scenarios;
    }

    public Set<Scenario> scenarios() {
        return _scenarios;
    }

    /**
     * Merge & Permute new Scenarios into existing Scenarios
     * <p>
     * Applies each new Scenario on top of each existing Scenario,
     * overriding any existing features with the new Scenario's treatment.
     * <p>
     * Creates a permutation of the two collections and then de-duplicates:
     * With New Scenarios [a=on,b=on]&[a=off,b=on] and Existing Scenarios [a=on,b=on]&[a=on,b=off]&[a=on,c=on]
     * The Merged Scenarios would permute to: [a=on,b=on]&[a=off,b=on]&[a=on,b=on]&[a=off,b=on]&[a=on,b=on,c=on]&[a=off,b=on,c=on]
     * Then the return would deduplicate down to: [a=on,b=on]&[a=off,b=on]&[a=on,b=on,c=on]&[a=off,b=on,c=on]
     *
     * @param newScenarios The Scenarios to be merged in
     * @return Current Suite Object
     */
    public Suite merge(Set<Scenario> newScenarios) {
        Preconditions.checkNotNull(newScenarios);

        Set<Scenario> mergedScenarios = Sets.newHashSet();
        for (Scenario existingScenario : _scenarios) {
            for (Scenario newScenario : newScenarios) {
                Scenario scenario = new Scenario(existingScenario);
                scenario.merge(newScenario);
                mergedScenarios.add(scenario);
            }
        }

        // No Merged Scenarios means one collection was empty
        if (mergedScenarios.isEmpty()) {
            mergedScenarios.addAll(_scenarios);
            mergedScenarios.addAll(newScenarios);
        }

        _scenarios = mergedScenarios;

        return this;
    }

    public Suite merge(Suite suite) {
        return merge(suite.scenarios());
    }

    /**
     * Validates there is only one Scenario in the Collection and that Scenario has no Tests defined
     *
     * @return If the only scenario in the collection has no tests defined
     */
    public boolean isOnlyAllControl() {
        return _scenarios == null
                || _scenarios.isEmpty()
                || (_scenarios.size() == 1 && _scenarios.iterator().next().tests().isEmpty());
    }
}
