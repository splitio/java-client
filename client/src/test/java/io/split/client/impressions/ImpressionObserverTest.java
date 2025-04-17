package io.split.client.impressions;

import com.google.common.base.Strings;
import org.junit.Ignore;
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

    private static final Logger _log = LoggerFactory.getLogger(ImpressionsManagerImpl.class);

    // We allow the cache implementation to have a 0.01% drift in size when elements change, given that it's internal
    // structure/references might vary, and the ObjectSizeCalculator is not 100% accurate
    private static final double SIZE_DELTA = 0.01;
    private final Random _rand = new Random();

    private List<Impression> generateImpressions(long count) {
        ArrayList<Impression> imps = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            Impression imp = new Impression(String.format("key_%d", i),
                    null,
                    String.format("feature_%d", i % 10),
                    (i % 2 == 0) ? "on" : "off",
                    System.currentTimeMillis(),
                    (i % 2 == 0) ? "in segment all" : "whitelisted",
                    i * i,
                    null,
                    null);
            imps.add(imp);
        }
        return imps;
    }

    @Test
    public void testBasicFunctionality() {
        ImpressionObserver observer = new ImpressionObserver(5);
        Impression imp = new Impression("someKey",
                null, "someFeature",
                "on", System.currentTimeMillis(),
                "in segment all",
                1234L,
                null,
                null);

        // Add 5 new impressions so that the old one is evicted and re-try the test.
        for (Impression i : generateImpressions(5)) {
            observer.testAndSet(i);
        }
        assertNull(observer.testAndSet(imp));
        assertThat(observer.testAndSet(imp), is(imp.time()));
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
            // TODO: Fix this test for JDK > 8
            return;
        }

        ImpressionObserver observer = new ImpressionObserver(500000);
        List<Impression> imps = generateImpressions(1000000);

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


    private void caller(ImpressionObserver o, int count, ConcurrentLinkedQueue<Impression> imps) {

        while (count-- > 0) {
            Impression i = new Impression("key_" + _rand.nextInt(100),
                    null,
                    "feature_" + _rand.nextInt(10),
                    _rand.nextBoolean() ? "on" : "off",
                    System.currentTimeMillis(),
                    "label" + _rand.nextInt(5),
                    1234567L,
                    null,
                    null);

            i = i.withPreviousTime(o.testAndSet(i));
            imps.offer(i);
        }
    }

    @Test
    @Ignore //Run locally is posible but has problem when run on Travis.
    public void testConcurrencyVsAccuracy() throws InterruptedException {
        ImpressionObserver observer = new ImpressionObserver(500000);
        ConcurrentLinkedQueue<Impression> imps = new ConcurrentLinkedQueue<>();
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
        for (Impression i : imps) {
            assertThat(i.pt(), is(anyOf(nullValue(), lessThanOrEqualTo(i.time()))));
        }
    }
}
