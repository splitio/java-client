package io.split.client.testing.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.split.client.testing.SplitClientForTest;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class StepDefinitions {
    private final SplitClientForTest splitClient = new SplitClientForTest();
    private final CoffeeMachine coffeeMachine = new CoffeeMachine(splitClient, "arbitraryKey");

    // Called by Cucumber to convert each row in the data table in the .feature file to a SKU object
    @DataTableType
    public SKU sku(Map<String, String> entry) {
        return new SKU(
                entry.get("name"),
                Double.parseDouble(entry.get("price"))
        );
    }

    @Given("the machine is not empty")
    public void the_machine_is_not_empty() {
        coffeeMachine.setLevel(1.0);
    }

    @Given("the machine is empty")
    public void the_machine_is_empty() {
        coffeeMachine.setLevel(0);
    }

    @Then("the following drinks should be available:")
    public void the_following_drinks_should_be_available(List<SKU> expectedSKUs) {
        List<SKU> availableSKUs = coffeeMachine.getAvailableDrinks();
        assertEquals(expectedSKUs, availableSKUs);
    }

    @Then("no drinks should be available")
    public void no_drinks_should_be_available() {
        List<SKU> availableSKUs = coffeeMachine.getAvailableDrinks();
        assertEquals(emptyList(), availableSKUs);
    }

    @Before
    public void configureSplit(Scenario scenario) {
        CucumberSplit.configureSplit(splitClient, scenario);
    }
}
