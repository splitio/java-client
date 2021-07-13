package io.split.client.testing.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.split.client.testing.SplitClientForTest;

import static org.junit.Assert.assertEquals;

public class StepDefinitions {
    private final SplitClientForTest splitClient = new SplitClientForTest();

    @Then("split {string} should be {string}")
    public void split_should_be(String split, String expectedValue) {
        assertEquals(expectedValue, splitClient.getTreatment("arbitraryKey", split));
    }

    @Before
    public void configureSplit(Scenario scenario) {
        CucumberSplit.configureSplit(splitClient, scenario);
    }
}
