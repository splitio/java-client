package io.split.client.testing;

import io.split.client.testing.annotations.SplitScenario;
import io.split.client.testing.annotations.SplitTest;
import io.split.client.testing.annotations.SplitTestClient;

class SplitAnnotationTestParent {
    static final String DEFAULT_PARENT_FEATURE = "DEFAULT_PARENT_FEATURE";
    static final String OVERRIDDEN_PARENT_FEATURE = "OVERRIDDEN_PARENT_FEATURE";

    private static final String ON_TREATMENT = "on";

    /**
     * Split Test Client with one Default Scenario
     * splitParent will be defined by the SplitTestRunner on initialization
     * All tests will run with these default SplitScenario(s),
     * Any SplitScenarios defined by inheriting classes will be applied over the default values
     * Any @Test level SplitTests and SplitScenarios applied over the default values
     * Any @Test level SplitTests and SplitScenarios will be run permuted across the SplitScenarios defined here
     */
    @SplitTestClient(scenarios = {
            @SplitScenario(tests = {
                    @SplitTest(feature = DEFAULT_PARENT_FEATURE, treatments = {ON_TREATMENT}),
                    @SplitTest(feature = OVERRIDDEN_PARENT_FEATURE, treatments = {ON_TREATMENT})
            })
    })
    private SplitClientForTest splitParent = new SplitClientForTest();
}
