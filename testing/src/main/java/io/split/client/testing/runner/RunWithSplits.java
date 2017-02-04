package io.split.client.testing.runner;

import com.google.common.collect.Maps;
import io.split.client.testing.SplitClientForTest;
import io.split.client.testing.annotations.SplitTestClient;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.Map;

public class RunWithSplits extends Statement {
    private final Object target;
    private final Scenario scenario;
    private final Statement next;

    public RunWithSplits(Scenario scenario, Object target, Statement next) {
        this.scenario = scenario;
        this.target = target;
        this.next = next;
    }

    @Override
    public void evaluate() throws Throwable {
        SplitClientForTest splitClient = findFirstSplitClient(target, target.getClass());

        // Preserve the Split state between Test runs
        Map<String, String> priorTests = Maps.newHashMap(splitClient.tests());

        // Apply the Active Scenario for this
        if (scenario != null) {
            scenario.apply(splitClient);
        }

        next.evaluate();

        // Clear any Scenario specific changes and re-apply existing splits
        splitClient.clearTreatments();
        splitClient.registerTreatments(priorTests);
    }

    private static SplitClientForTest findFirstSplitClient(Object target, Class<?> type) throws IllegalAccessException {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(SplitTestClient.class)
                    && field.getType().isAssignableFrom(SplitClientForTest.class)) {
                field.setAccessible(true); // should work on private fields
                if (field.get(target) instanceof SplitClientForTest) {
                    return (SplitClientForTest) field.get(target);
                }
            }
        }

        if (type.getSuperclass() != null) {
            return findFirstSplitClient(target, type.getSuperclass());
        } else {
            throw new IllegalArgumentException("No SplitTestClient found in hierarchy");
        }
    }
}