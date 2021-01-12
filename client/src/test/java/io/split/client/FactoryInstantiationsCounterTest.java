package io.split.client;

import junit.framework.TestCase;
import org.junit.Test;

public class FactoryInstantiationsCounterTest extends TestCase {

    private static final String FIRST_TOKEN = "TOKENNUMBER1";
    private static final String SECOND_TOKEN = "TOKENNUMBER2";

    @Test
    public void testAddingNewToken() {
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().addToken(FIRST_TOKEN);
        assertTrue(FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().isTokenPresent(FIRST_TOKEN));

        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().removeToken(FIRST_TOKEN);
    }

    @Test
    public void testAddingExistingToken() {
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().addToken(FIRST_TOKEN);
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().addToken(FIRST_TOKEN);

        assertTrue(FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().isTokenPresent(FIRST_TOKEN));
        assertEquals(2, FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().getCount(FIRST_TOKEN));
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().removeToken(FIRST_TOKEN);
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().removeToken(FIRST_TOKEN);
    }

    @Test
    public void testRemovingToken() {
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().addToken(FIRST_TOKEN);
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().removeToken(FIRST_TOKEN);

        assertFalse(FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().isTokenPresent(FIRST_TOKEN));
        assertEquals(0, FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().getCount(FIRST_TOKEN));
    }

    @Test
    public void testAddingNonExistingToken() {
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().addToken(FIRST_TOKEN);
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().addToken(SECOND_TOKEN);

        assertTrue(FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().isTokenPresent(FIRST_TOKEN));
        assertEquals(1, FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().getCount(FIRST_TOKEN));
        assertEquals(1, FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().getCount(SECOND_TOKEN));
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().removeToken(FIRST_TOKEN);
        FactoryInstantiationsCounter.getFactoryInstantiationsServiceInstance().removeToken(SECOND_TOKEN);
    }
}
