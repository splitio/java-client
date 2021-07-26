package io.split.client.testing.cucumber;

import io.split.client.SplitClient;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * A simple coffee machine that displays available drinks. It can offer an experimental cappuccino
 * drink that is toggled on/off with Split.
 */
public class CoffeeMachine {
    private final SplitClient splitClient;
    private final String splitKey;
    private double level;

    public CoffeeMachine(SplitClient splitClient, String splitKey) {
        this.splitClient = splitClient;
        this.splitKey = splitKey;
    }

    /**
     * Indicate how full the machine is
     *
     * @param level a number between 0 and 1
     */
    public void setLevel(double level) {
        this.level = level;
    }

    public List<SKU> getAvailableDrinks() {
        if(this.level == 0) return emptyList();

        List<SKU> availableDrinks = new ArrayList<>();
        availableDrinks.add(new SKU("filter coffee", 0.80));
        if ("on".equals(this.splitClient.getTreatment(splitKey, "cappuccino"))) {
            availableDrinks.add(new SKU("cappuccino", 1.10));
        }
        return unmodifiableList(availableDrinks);
    }
}
