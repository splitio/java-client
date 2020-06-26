package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;
// import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ImpressionObserverTest {

    private static final Logger _log = LoggerFactory.getLogger(ImpressionsManager.class);

    // We allow the cache implementation to have a 0.01% drift in size when elements change, given that it's internal
    // structure/references might vary, and the ObjectSizeCalculator is not 100% accurate
    private static final double SIZE_DELTA = 0.01;

    private List<KeyImpression> generateKeyImpressions(long count) {
        ArrayList<KeyImpression> imps = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            KeyImpression imp = new KeyImpression();
            imp.keyName = String.format("key_%d", i);
            imp.feature = String.format("feature_%d", i % 10);
            imp.label = (i % 2 == 0) ? "in segment all" : "whitelisted";
            imp.changeNumber = i * i;
            imp.time = System.currentTimeMillis();
            imps.add(imp);
        }
        return imps;
    }

    @Test
    public void testBasicFunctionality() {
        ImpressionObserver observer = new ImpressionObserver(5);
        KeyImpression imp = new KeyImpression();
        imp.keyName = "someKey";
        imp.feature = "someFeature";
        imp.label = "in segment all";
        imp.changeNumber = 1234L;
        imp.time = System.currentTimeMillis();


        // Add 5 new impressions so that the old one is evicted and re-try the test.
        for (KeyImpression ki : generateKeyImpressions(5)) {
            observer.testAndSet(ki);
        }
        assertNull(observer.testAndSet(imp));
        assertThat(observer.testAndSet(imp), is(imp.time));
    }

    @Test
    public void testMemoryUsageStopsWhenCacheIsFull() throws InvocationTargetException, IllegalAccessException {

        Class objectSizeCalculatorClass;
        ClassLoader classLoader = this.getClass().getClassLoader();
        Method getObjectSize;
        try {
            objectSizeCalculatorClass = classLoader.loadClass("jdk.nashorn.internal.ir.debug.ObjectSizeCalculator");
            getObjectSize = objectSizeCalculatorClass.getMethod("getObjectSize", Object.class);    //getObjectSize(observer);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            _log.error("This test only runs with the hotspot JVM. PLEASE ensure it runs correctly at least locally. " +
                    "Ignoring test now.");
            return;
        }

        ImpressionObserver observer = new ImpressionObserver(500000);
        List<KeyImpression> imps = generateKeyImpressions(1000000);

        for (int index = 0; index < 500000; index++) { // fill the cache with half the generated key impressions
            observer.testAndSet(imps.get(index));
        }

        long sizeAfterInitialPopulation = (long) getObjectSize.invoke(null, observer);

        for (int index = 500000; index < 1000000; index++) { // re-fill the cache with the rest of the generated key impressions
            observer.testAndSet(imps.get(index));
        }

        long sizeAfterSecondPopulation = (long) getObjectSize.invoke(null, observer);

        assertThat((double) (sizeAfterSecondPopulation - sizeAfterInitialPopulation), lessThan (SIZE_DELTA * sizeAfterInitialPopulation));

    }
}
