package io.split.client;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ApiKeyCounterTest extends TestCase {

    private static final String FIRST_KEY = "KEYNUMBER1";
    private static final String SECOND_KEY = "KEYNUMBER2";

    @After
    public synchronized void clearApiKeys() {
        ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
    }

    @Test
    public synchronized void testAddingNewToken() {
        try {
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            assertTrue(ApiKeyCounter.getApiKeyCounterInstance().isApiKeyPresent(FIRST_KEY));
        }
        finally {
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        }
    }

    @Test
    public synchronized void testAddingExistingToken() {
        try {
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);

            assertTrue(ApiKeyCounter.getApiKeyCounterInstance().isApiKeyPresent(FIRST_KEY));
            assertEquals(2, ApiKeyCounter.getApiKeyCounterInstance().getCount(FIRST_KEY));
        }
        finally {
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        }
    }

    @Test
    public synchronized void testRemovingToken() {
        try {
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().remove(FIRST_KEY);

            assertFalse(ApiKeyCounter.getApiKeyCounterInstance().isApiKeyPresent(FIRST_KEY));
            assertEquals(0, ApiKeyCounter.getApiKeyCounterInstance().getCount(FIRST_KEY));
        }
        finally {
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        }
    }

    @Test
    public synchronized void testAddingNonExistingToken() {
        try {
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(SECOND_KEY);

            assertTrue(ApiKeyCounter.getApiKeyCounterInstance().isApiKeyPresent(FIRST_KEY));
            assertEquals(1, ApiKeyCounter.getApiKeyCounterInstance().getCount(FIRST_KEY));
            assertEquals(1, ApiKeyCounter.getApiKeyCounterInstance().getCount(SECOND_KEY));
        }
        finally {
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        }
    }

    @Test
    public synchronized void testFactoryInstances() {
        try {
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(SECOND_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(SECOND_KEY);

            Map<String, Long> factoryInstances = ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances();
            Assert.assertEquals(2, factoryInstances.size());
            Assert.assertEquals(3, factoryInstances.get(FIRST_KEY).intValue());
            Assert.assertEquals(2, factoryInstances.get(SECOND_KEY).intValue());
        }
        finally {
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        }
    }

    @Test
    public synchronized void testClearApiKey() {
        try {
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
            Assert.assertEquals(0, ApiKeyCounter.getApiKeyCounterInstance().getCount(FIRST_KEY));
        }
        finally {
            ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        }
    }
}
