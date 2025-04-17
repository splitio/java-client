package io.split.client.testing;

import io.split.client.api.Key;
import io.split.client.dtos.EvaluationOptions;
import io.split.client.testing.annotations.SplitScenario;
import io.split.client.testing.annotations.SplitSuite;
import io.split.client.testing.annotations.SplitTest;
import io.split.client.testing.annotations.SplitTestClient;
import io.split.client.testing.runner.SplitTestRunner;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * SplitScenarioAnnotationTest
 * Demonstration & Test file for the use of SplitScenarios with the SplitTestRunner
 * SplitTestRunner is a JUnitTestRunner which executes annotated tests to achieve a variety of Split configurations
 */
@RunWith(SplitTestRunner.class)
public class SplitScenarioAnnotationTest extends SplitAnnotationTestParent {
    private static final String ARBITRARY_KEY = "key";

    private static final String DEFAULT_CLIENT_FEATURE = "DEFAULT_CLIENT_FEATURE";
    private static final String CONTROL_FEATURE = "CONTROL_FEATURE";

    private static final String CONTROL_TREATMENT = "control";
    private static final String ON_TREATMENT = "on";
    private static final String OFF_TREATMENT = "off";

    /**
     * Split Test Client with one Default Scenario
     * This splitClient will be used instead of the parent's
     * All tests will run with these default client SplitScenario(s),
     * Any SplitTestClient SplitScenarios defined by inheriting classes will be applied over the default values
     * Any @Test level SplitTests and SplitScenarios applied over the default values
     * Any @Test level SplitTests and SplitScenarios will be run permuted across the SplitScenarios defined here
     */
    @SplitTestClient(scenarios = {
            @SplitScenario(tests = {
                    @SplitTest(feature = DEFAULT_CLIENT_FEATURE, treatments = {ON_TREATMENT}),
                    @SplitTest(feature = OVERRIDDEN_PARENT_FEATURE, treatments = {OFF_TREATMENT})
            })
    })
    private SplitClientForTest splitClient = new SplitClientForTest();


    /**
     * Test confirming the application of the SplitTestClient level default SplitScenario
     * This test will run once
     * Only the tests in this SplitClient's and the Parent's SplitClient's SplitScenarios above are set
     * The DEFAULT_CLIENT_FEATURE returns the treatment set on SplitTestClient
     * The OVERRIDDEN_PARENT_FEATURE returns the treatment set on SplitTestClient
     * The CONTROL_FEATURE returns the control treatment as it was never set
     */
    @Test
    public void testDefaultScenario() {
        Assert.assertEquals(3, splitClient.tests().size());
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_PARENT_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE, new HashMap<>()));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(new Key(ARBITRARY_KEY, ARBITRARY_KEY), DEFAULT_CLIENT_FEATURE, new HashMap<>()));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatmentWithConfig(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE, new HashMap<>()).treatment());
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatmentWithConfig(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE).treatment());
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatmentWithConfig(new Key(ARBITRARY_KEY, ARBITRARY_KEY), DEFAULT_CLIENT_FEATURE, new HashMap<>()).treatment());
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatments(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE)).get(DEFAULT_CLIENT_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatments(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE),  new HashMap<>()).get(DEFAULT_CLIENT_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatments(new Key(ARBITRARY_KEY, ARBITRARY_KEY), Arrays.asList(DEFAULT_CLIENT_FEATURE),  new HashMap<>()).get(DEFAULT_CLIENT_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatmentsWithConfig(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE)).get(DEFAULT_CLIENT_FEATURE).treatment());
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatmentsWithConfig(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE),  new HashMap<>()).get(DEFAULT_CLIENT_FEATURE).treatment());
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatmentsWithConfig(new Key(ARBITRARY_KEY, ARBITRARY_KEY), Arrays.asList(DEFAULT_CLIENT_FEATURE),  new HashMap<>()).get(DEFAULT_CLIENT_FEATURE).treatment());
        Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, OVERRIDDEN_PARENT_FEATURE));
        Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));

        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSet(ARBITRARY_KEY, "flagset",  new HashMap<>()));
        Assert.assertEquals(null, splitClient.getTreatmentsByFlagSet(ARBITRARY_KEY, "flagset"));
        Assert.assertEquals(null, splitClient.getTreatmentsByFlagSet(ARBITRARY_KEY, "flagset"));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"),  new HashMap<>()));
        Assert.assertEquals(null, splitClient.getTreatmentsByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset")));
        Assert.assertEquals(null, splitClient.getTreatmentsByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset")));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSet(ARBITRARY_KEY, "flagset",  new HashMap<>()));
        Assert.assertEquals(null, splitClient.getTreatmentsWithConfigByFlagSet(ARBITRARY_KEY, "flagset"));
        Assert.assertEquals(null, splitClient.getTreatmentsWithConfigByFlagSet(ARBITRARY_KEY, "flagset"));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"),  new HashMap<>()));
        Assert.assertEquals(null, splitClient.getTreatmentsWithConfigByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset")));
        Assert.assertEquals(null, splitClient.getTreatmentsWithConfigByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset")));

        Assert.assertEquals(null, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE, new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(null, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE, new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(null, splitClient.getTreatment(new Key(ARBITRARY_KEY, ARBITRARY_KEY), DEFAULT_CLIENT_FEATURE, new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(null, splitClient.getTreatmentWithConfig(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE, new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(null, splitClient.getTreatmentWithConfig(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE, new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(null, splitClient.getTreatmentWithConfig(new Key(ARBITRARY_KEY, ARBITRARY_KEY), DEFAULT_CLIENT_FEATURE, new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatments(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatments(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE),  new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfig(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(null, splitClient.getTreatmentsWithConfig(ARBITRARY_KEY, Arrays.asList(DEFAULT_CLIENT_FEATURE),  new HashMap<>(), new EvaluationOptions(new HashMap<>())).get(DEFAULT_CLIENT_FEATURE));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfig(new Key(ARBITRARY_KEY, ARBITRARY_KEY), Arrays.asList(DEFAULT_CLIENT_FEATURE),  new HashMap<>(), new EvaluationOptions(new HashMap<>())));

        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSet(ARBITRARY_KEY, "flagset",  new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSet(ARBITRARY_KEY, "flagset", new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSet(ARBITRARY_KEY, "flagset", new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"),  new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSet(ARBITRARY_KEY, "flagset",  new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSet(ARBITRARY_KEY, "flagset", new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSet(ARBITRARY_KEY, "flagset", new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"),  new HashMap<>(), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"), new EvaluationOptions(new HashMap<>())));
        Assert.assertEquals(new HashMap<>(), splitClient.getTreatmentsWithConfigByFlagSets(ARBITRARY_KEY, Arrays.asList("flagset"), new EvaluationOptions(new HashMap<>())));
    }

    /**
     * Test confirming the SplitTestClient level default SplitScenario is overridden by @Test level SplitTest definitions
     * This test will run once
     * Only the tests in this SplitClient's and the Parent's SplitClient's SplitScenarios above are set with an override by the Tests SplitTest
     * The DEFAULT_CLIENT_FEATURE returns the treatment set in the SplitTest
     * The CONTROL_FEATURE returns the control treatment as it was never set
     */
    @Test
    @SplitTest(feature = DEFAULT_CLIENT_FEATURE, treatments = {OFF_TREATMENT})
    public void testDefaultOverrideSingle() {
        Assert.assertEquals(3, splitClient.tests().size());
        Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_PARENT_FEATURE));
        Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, OVERRIDDEN_PARENT_FEATURE));

        Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE));
    }

    /**
     * Test confirming the SplitTestClient level default SplitScenario is overridden by @Test level SplitTest definitions
     * This test will run twice, once for each treatment in the SplitTest
     * Only the tests in this SplitClient's and the Parent's SplitClient's SplitScenarios above are set with an override by the Tests SplitTest
     * The DEFAULT_CLIENT_FEATURE returns the treatment set on SplitTestClient
     * The CONTROL_FEATURE returns the control treatment as it was never set
     */
    @Test
    @SplitTest(feature = DEFAULT_CLIENT_FEATURE, treatments = {OFF_TREATMENT, CONTROL_TREATMENT})
    public void testDefaultOverrideMultiple() {
        Assert.assertEquals(3, splitClient.tests().size());
        Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_PARENT_FEATURE));
        Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, OVERRIDDEN_PARENT_FEATURE));

        Assert.assertThat(splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE),
                CoreMatchers.anyOf(CoreMatchers.is(OFF_TREATMENT), CoreMatchers.is(CONTROL_TREATMENT)));
    }

    /**
     * Test running a specific SplitScenario
     * This test will run once, using a merge of the Tests SplitScenario and the SplitTestClient's SplitScenario
     * The one SplitTestClient test and the two local SplitScenario tests are set
     * The DEFAULT_CLIENT_FEATURE returns the treatment set on SplitTestClient
     * The CONTROL_FEATURE returns the control treatment as it was never set
     * The SCENARIO_FEATURE_1 & SCENARIO_FEATURE_2 return the treatment set by the test's SplitScenario
     */
    @Test
    @SplitScenario(tests = {
            @SplitTest(feature = "SCENARIO_FEATURE_1", treatments = {ON_TREATMENT}),
            @SplitTest(feature = "SCENARIO_FEATURE_2", treatments = {OFF_TREATMENT})
    })
    public void testScenarioAnnotation() {
        Assert.assertEquals(5, splitClient.tests().size());
        Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_FEATURE_1"));
        Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_FEATURE_2"));
    }

    /**
     * Test running a specific SplitScenario
     * This test will run twice, each time a merging one of the Test's SplitScenarios and the SplitTestClient's SplitScenario
     * The CONTROL_FEATURE returns the control treatment as it was never set
     * In the first scenario SCENARIO_FEATURE_1 & SCENARIO_FEATURE_2 are set
     * In the first scenario SCENARIO_FEATURE_1, SCENARIO_FEATURE_2 & SCENARIO_FEATURE_3 are set and DEFAULT_CLIENT_FEATURE is overridden
     */
    @Test
    @SplitSuite(scenarios = {
            @SplitScenario(tests = {
                    @SplitTest(feature = "SCENARIO_NUMBER", treatments = {"1"}),
                    @SplitTest(feature = "SCENARIO_FEATURE_1", treatments = {ON_TREATMENT}),
                    @SplitTest(feature = "SCENARIO_FEATURE_2", treatments = {OFF_TREATMENT})
            }),
            @SplitScenario(tests = {
                    @SplitTest(feature = "SCENARIO_NUMBER", treatments = {"2"}),
                    @SplitTest(feature = "SCENARIO_FEATURE_1", treatments = {ON_TREATMENT}),
                    @SplitTest(feature = "SCENARIO_FEATURE_2", treatments = {ON_TREATMENT}),
                    @SplitTest(feature = "SCENARIO_FEATURE_3", treatments = {ON_TREATMENT}),
                    @SplitTest(feature = DEFAULT_CLIENT_FEATURE, treatments = {OFF_TREATMENT})
            })
    })
    public void testScenariosAnnotation() {
        Assert.assertThat(splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_NUMBER"),
                CoreMatchers.anyOf(CoreMatchers.is("1"), CoreMatchers.is("2")));
        if (Objects.equals("1", splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_NUMBER"))) {
            Assert.assertEquals(6, splitClient.tests().size());
            Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));
            Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_PARENT_FEATURE));
            Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, OVERRIDDEN_PARENT_FEATURE));

            Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE));
            Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_FEATURE_1"));
            Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_FEATURE_2"));
        } else if (Objects.equals("2", splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_NUMBER"))) {
            Assert.assertEquals(7, splitClient.tests().size());
            Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));
            Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_PARENT_FEATURE));
            Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, OVERRIDDEN_PARENT_FEATURE));

            Assert.assertEquals(OFF_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, DEFAULT_CLIENT_FEATURE));
            Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_FEATURE_1"));
            Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_FEATURE_2"));
            Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SCENARIO_FEATURE_3"));
        }
    }
}