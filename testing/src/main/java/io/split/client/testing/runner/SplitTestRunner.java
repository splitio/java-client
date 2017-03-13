package io.split.client.testing.runner;

import io.split.client.testing.SplitClientForTest;
import io.split.client.testing.annotations.SplitScenario;
import io.split.client.testing.annotations.SplitSuite;
import io.split.client.testing.annotations.SplitTest;
import io.split.client.testing.annotations.SplitTestClient;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Split Test Runner
 * <p>
 * Extension of standard JUnit 4 Runner
 * Leverages Split* Testing Annotations to automate unit testing across various Feature Flag treatments
 * Only one Annotation is respected per Test (IE Can not combine @SplitTest with @SplitScenarios on a single Test method)
 */
public class SplitTestRunner extends BlockJUnit4ClassRunner {
    private Suite _globalSplitSuite;
    private Scenario _activeScenario;

    /**
     * Split Test Runner Constructor
     * <p>
     * Runner is instantiated by using the @RunWith(SplitTestRunner.class) annotation.
     * Not to be called directly.
     *
     * @param klass The Test class to be run
     * @throws InitializationError Error occurred during class initialization
     */
    public SplitTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    /**
     * Run tests in Alphabetical Order
     * <p>
     * Runner is instantiated by using the @RunWith(SplitTestRunner.class) annotation.
     * Not to be called directly.
     */
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> methods = new ArrayList<>(super.computeTestMethods());
        Collections.sort(methods, new Comparator<FrameworkMethod>() {
            @Override
            public int compare(FrameworkMethod o1, FrameworkMethod o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return methods;
    }

    /**
     * Run Suite of Scenarios for a Test
     * <p>
     * Generates a Suite of Scenarios and then runs the test once for each permutation.
     * If no Scenarios are defined, or the only defined Scenario has no Tests set, Run test normally
     *
     * @param method   The Test function to be run
     * @param notifier The notifier class tracking test execution
     */
    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        if (isIgnored(method)) {
            notifier.fireTestIgnored(describeChild(method));
            return;
        }

        Suite suite = generateSuite(method);
        if (suite.isOnlyAllControl()) {
            // Treat All-Control scenario as normal test
            runLeaf(methodBlock(method), describeChild(method), notifier);
        } else {
            for (Scenario scenario : suite.scenarios()) {
                // Apply Scenario Splits and Run Test
                _activeScenario = scenario;
                runLeaf(methodBlock(method), describeScenarioTest(method, scenario), notifier);
                _activeScenario = null;
            }
        }
    }

    /**
     * Adds Split Application to the Test's Call Stack
     * <p>
     * This Override method first adds the application of Splits to run before the test execution.
     * Then calls the SuperClass's method to add all @Before annotations to the call stack
     * <p>
     * This is required to ensure the Tested code is using the same SplitClient that the Runner is using.
     *
     * @return An instance of the Test Class being run
     */
    @Override
    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        Statement withSplits = new RunWithSplits(_activeScenario, target, statement);
        return super.withBefores(method, target, withSplits);
    }

    public Suite generateSuite(FrameworkMethod method) {
        Suite globalSuite = getGlobalSuiteCopy();

        // Generate Scenarios or Default to the empty Scenario
        Suite methodSuite;
        if (method.getAnnotation(SplitTest.class) != null) {
            methodSuite = new Suite(method.getAnnotation(SplitTest.class));
        } else if (method.getAnnotation(SplitScenario.class) != null) {
            methodSuite = new Suite(method.getAnnotation(SplitScenario.class));
        } else if (method.getAnnotation(SplitSuite.class) != null) {
            methodSuite = new Suite(method.getAnnotation(SplitSuite.class));
        } else {
            methodSuite = new Suite();
        }

        return globalSuite.merge(methodSuite);
    }

    /**
     * Get Global Split Suite
     * <p>
     * Parses Suite assigned in the SplitTestClient annotation.
     * Allows all tests to perform a permutation of all scenarios in the Suite
     * and the Tests or Scenarios defined at that Tests level
     * This function is only run once and then the result is stored within the Runner.
     *
     * @return The Suite of Scenarios
     */
    private Suite getGlobalSuiteCopy() {
        if (_globalSplitSuite == null) {
            _globalSplitSuite = cascadeScenarios(getTestClass().getJavaClass());
        }

        return new Suite(_globalSplitSuite);
    }

    /**
     * Merges SplitScenarios from SplitTestClient annotation throughout hierarchy
     * Recurse down to the oldest ancestor, merges any Client Scenarios onto the Module Scenarios
     * and then repeats for the descendant class until returning to the Test Class.
     * <p>
     * If Test.java defines SplitModule Scenarios [a=on,b=off]&[a=on,b=on] and inherits from BaseTest.java which defines
     * SplitClient Scenario [a=off,c=on,d=on] and SplitModule Scenario [d=off],
     * The Cascaded Senarios would be to inherit from prior scenarios giving the final set of Scenarios:
     * [a=on,b=off,c=on,d=on]&[a=on,b=on,c=on,d=on]
     *
     * @param type Test Class or Super Class
     * @return Merged Scenarios
     */
    private Suite cascadeScenarios(Class<?> type) {
        Suite suite;
        if (type.getSuperclass() != null) {
            suite = cascadeScenarios(type.getSuperclass());
        } else {
            suite = new Suite();
        }

        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(SplitTestClient.class)) {
                if (field.getType().isAssignableFrom(SplitClientForTest.class)) {
                    suite.merge(new Suite(field.getAnnotation(SplitTestClient.class)));
                }
            }
        }

        return suite;
    }

    /**
     * Generate Test Description
     * <p>
     * If no Scenarios are on for that test, describe normally
     * Otherwise Describe as a Suite of tests, showing the active splits for each Scenario in that run's description
     *
     * @param method The Test method to be run
     * @return A formatted description of that Test
     */
    @Override
    protected Description describeChild(FrameworkMethod method) {
        /*
         * When Running a single test by name, Java tries to find that test by matching it against the description
         * However that description is generated using the default logic (not the Runner's)
         * To preserve the ability to run single tests, this function reverts
         * to the default behavior when called by the shouldRun function
         */
        if (Objects.equals("shouldRun", Thread.currentThread().getStackTrace()[3].getMethodName())) {
            return super.describeChild(method);
        }

        Suite suite = generateSuite(method);
        if (!suite.isOnlyAllControl()) {
            Description description = Description.createSuiteDescription(
                    testName(method),
                    method.getAnnotations()
            );

            for (Scenario scenario : suite.scenarios()) {
                description.addChild(describeScenarioTest(method, scenario));
            }

            return description;
        } else {
            return super.describeChild(method);
        }
    }

    private Description describeScenarioTest(FrameworkMethod method, Scenario scenario) {
        // Using a SuiteDescription here rather than TestDescription because TestDescription prepends the Class name
        return Description.createSuiteDescription("[" + scenario.toString() + "] : " + testName(method));
    }
}
