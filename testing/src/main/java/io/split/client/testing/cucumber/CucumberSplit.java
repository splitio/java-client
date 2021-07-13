package io.split.client.testing.cucumber;

import io.cucumber.java.Scenario;
import io.split.client.testing.SplitClientForTest;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Simple Cucumber plugin for Split.
 * </p>
 * <p>
 * Cucumber scenarios annotated with {@code @split[feature:treatment]} tags can be used to
 * configure a {@link SplitClientForTest} instance.
 * </p>
 * <p>
 * To use it, define a <a href="">Before Hook</a> that invokes the {@link CucumberSplit#configureSplit(SplitClientForTest, Scenario)}
 * method. Example:
 * </p>
 *
 * <pre>
 * import io.cucumber.java.Before;
 * import io.split.client.testing.SplitClientForTest;
 *
 * public class StepDefinitions {
 *     private final SplitClientForTest splitClient = new SplitClientForTest();
 *
 *     &#64;Before
 *     public void configureSplit(Scenario scenario) {
 *         CucumberSplit.configureSplit(splitClient, scenario);
 *     }
 * }
 * </pre>
 */
public class CucumberSplit {
    private static final Pattern SPLIT_TAG_PATTERN = Pattern.compile("^@split\\[(.*):(.*)]");

    public static void configureSplit(SplitClientForTest splitClient, Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();
        for (String tag : tags) {
            Matcher matcher = SPLIT_TAG_PATTERN.matcher(tag);
            if (matcher.matches()) {
                String feature = matcher.group(1);
                String treatment = matcher.group(2);
                splitClient.registerTreatment(feature, treatment);
            }
        }
    }
}
