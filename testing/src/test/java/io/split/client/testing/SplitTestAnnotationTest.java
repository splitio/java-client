package io.split.client.testing;

import io.split.client.testing.annotations.SplitScenario;
import io.split.client.testing.annotations.SplitTest;
import io.split.client.testing.annotations.SplitTestClient;
import io.split.client.testing.runner.SplitTestRunner;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SplitTestAnnotationTest
 * Demonstration & Test file for the use of SplitTests with the SplitTestRunner
 * SplitTestRunner is a JUnitTestRunner which executes annotated tests to achieve a variety of Split config
 */
@RunWith(SplitTestRunner.class)
public class SplitTestAnnotationTest {
    private static final String CONTROL_FEATURE = "CONTROL_FEATURE";
    private static final String MANUAL_FEATURE = "MANUAL_FEATURE";

    private static final String CONTROL_TREATMENT = "control";
    private static final String ON_TREATMENT = "on";
    private static final String OFF_TREATMENT = "off";

    /**
     * The SplitClientForTest will return the same treatment for a feature for all keys
     * The String chosen here is completeley arbitrary
     */
    private static final String ARBITRARY_KEY = "key";

    /**
     * Split Test Client with one Default Scenario
     * By default, any Feature will return the Control treatment for all keys
     */
    @SplitTestClient
    private static SplitClientForTest splitClient = new SplitClientForTest();

    /**
     * Validate that SplitTestClient tests do not persist after tests
     */
    @AfterClass
    public static void confirmUnchanged() {
        Assert.assertTrue(splitClient.tests().isEmpty());
    }

    /**
     * Confirm @Ignore annotation is respected by SplitTestRunner
     * Test will fail if run
     */
    @Test
    @Ignore
    public void testIgnore() {
        Assert.assertTrue(false);
    }

    /**
     * Test Manually setting of Treatments using the SplitClientForTest.registerTreatment() function
     */
    @Test
    public void testManualRegistration() {
        splitClient.registerTreatment(MANUAL_FEATURE, ON_TREATMENT);
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, MANUAL_FEATURE));
        Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));
        Assert.assertEquals(1, splitClient.tests().size());
        splitClient.clearTreatments();
    }

    /**
     * Test the SplitTest annotation will set the defined feature and treatment
     * This test runs once, setting the one specified treatment as defined in the annotation
     */
    @Test
    @SplitTest(feature = "SPLIT_TEST_FEATURE", treatments = {ON_TREATMENT})
    public void testSplitTestAnnotationSingle() {
        Assert.assertEquals(1, splitClient.tests().size());
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SPLIT_TEST_FEATURE"));
    }

    /**
     * Test the SplitTest annotation will set the defined feature and treatments
     * This test runs twice, setting the two different treatments for the feature as defined in the annotation
     */
    @Test
    @SplitTest(feature = "SPLIT_TEST_MULTIPLE_FEATURE", treatments = {ON_TREATMENT, OFF_TREATMENT})
    public void testSplitTestAnnotationMultiple() {
        Assert.assertEquals(1, splitClient.tests().size());
        String treatment = splitClient.getTreatment(ARBITRARY_KEY, "SPLIT_TEST_MULTIPLE_FEATURE");
        Assert.assertNotEquals(CONTROL_TREATMENT, treatment);
        Assert.assertTrue(ON_TREATMENT.equals(treatment) || OFF_TREATMENT.equals(treatment));
    }


    /**
     * Test the SplitScenario annotation will properly set multiple SplitTest features and treatments
     * This test runs four times, setting the features with all permutations of the defined treatments
     */
    @Test
    @SplitScenario(tests = {
            @SplitTest(feature = "SPLIT_TESTS_1", treatments = {ON_TREATMENT, OFF_TREATMENT}),
            @SplitTest(feature = "SPLIT_TESTS_2", treatments = {ON_TREATMENT, CONTROL_TREATMENT}),
            @SplitTest(feature = "SPLIT_TESTS_3", treatments = {ON_TREATMENT})
    })
    public void testSplitTestsAnnotation() {
        Assert.assertEquals(3, splitClient.tests().size());
        Assert.assertEquals(CONTROL_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, CONTROL_FEATURE));
        Assert.assertThat(splitClient.getTreatment(ARBITRARY_KEY, "SPLIT_TESTS_1"),
                CoreMatchers.anyOf(CoreMatchers.is(ON_TREATMENT), CoreMatchers.is(OFF_TREATMENT)));
        Assert.assertThat(splitClient.getTreatment(ARBITRARY_KEY, "SPLIT_TESTS_2"),
                CoreMatchers.anyOf(CoreMatchers.is(ON_TREATMENT), CoreMatchers.is(CONTROL_TREATMENT)));
        Assert.assertEquals(ON_TREATMENT, splitClient.getTreatment(ARBITRARY_KEY, "SPLIT_TESTS_3"));
    }
}