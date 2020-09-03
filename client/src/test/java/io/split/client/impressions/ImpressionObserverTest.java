package io.split.client.impressions;

import com.google.common.base.Strings;
import io.split.client.dtos.KeyImpression;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
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
    public void testMemoryUsageStopsWhenCacheIsFull() throws Exception {

        Class objectSizeCalculatorClass;
        ClassLoader classLoader = this.getClass().getClassLoader();
        Method getObjectSize;
        try {
            objectSizeCalculatorClass = classLoader.loadClass("jdk.nashorn.internal.ir.debug.ObjectSizeCalculator");
            getObjectSize = objectSizeCalculatorClass.getMethod("getObjectSize", Object.class);    //getObjectSize(observer);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            _log.error("This test only runs with the hotspot JVM. It's ignored locally, but mandatory on CI");
            if (!Strings.isNullOrEmpty(System.getenv("CI"))) { // If the CI environment variable is present
                throw new Exception("Setup CI to run with a hotspot JVM");
            }
            // Otherwise just ignore this test.
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

        assertThat((double) (sizeAfterSecondPopulation - sizeAfterInitialPopulation), lessThan(SIZE_DELTA * sizeAfterInitialPopulation));
    }


    private void caller(ImpressionObserver o, int count, ConcurrentLinkedQueue<KeyImpression> imps) {
        Random rand = new Random();
        while (count-- > 0) {
            KeyImpression k = new KeyImpression();
            k.keyName = "key_" + rand.nextInt(100);
            k.feature = "feature_" + rand.nextInt(10);
            k.label = "label" + rand.nextInt(5);
            k.treatment = rand.nextBoolean() ? "on" : "off";
            k.changeNumber = 1234567L;
            k.time = System.currentTimeMillis();
            k.pt = o.testAndSet(k);
            imps.offer(k);
        }
    }

    @Test
    public void testConcurrencyVsAccuracy() throws InterruptedException {
        ImpressionObserver observer = new ImpressionObserver(500000);
        ConcurrentLinkedQueue<KeyImpression> imps = new ConcurrentLinkedQueue<>();
        Thread t1 = new Thread(() -> caller(observer, 1000000, imps));
        Thread t2 = new Thread(() -> caller(observer, 1000000, imps));
        Thread t3 = new Thread(() -> caller(observer, 1000000, imps));
        Thread t4 = new Thread(() -> caller(observer, 1000000, imps));
        Thread t5 = new Thread(() -> caller(observer, 1000000, imps));

        // start the 5 threads an wait for them to finish.
        t1.setDaemon(true); t2.setDaemon(true); t3.setDaemon(true); t4.setDaemon(true); t5.setDaemon(true);
        t1.start(); t2.start(); t3.start(); t4.start(); t5.start();
        t1.join(); t2.join(); t3.join(); t4.join(); t5.join();

        assertThat(imps.size(), is(equalTo(5000000)));
        for (KeyImpression i : imps) {
            assertThat(i.pt, is(anyOf(nullValue(), lessThanOrEqualTo(i.time))));
        }
    }
}
