package io.split.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by adilaijaz on 4/27/16.
 */
public class SDKReadinessGates {
    private static final Logger _log = LoggerFactory.getLogger(SDKReadinessGates.class);

    private final CountDownLatch _splitsAreReady = new CountDownLatch(1);
    private final CountDownLatch _internalReady = new CountDownLatch(1);
    private final ConcurrentMap<String, CountDownLatch> _segmentsAreReady = new ConcurrentHashMap<>();

    /**
     * Returns true if the SDK is ready. The SDK is ready when:
     * <ol>
     * <li>It has fetched Split definitions the first time.</li>
     * <li>It has downloaded segment memberships for segments in use in the initial split definitions</li>
     * </ol>
     * <p/>
     * This operation will block until the SDK is ready or 'milliseconds' have passed. If the milliseconds
     * are less than or equal to zero, the operation will not block and return immediately
     *
     * @param milliseconds time to wait for an answer. if the value is zero or negative, we will not
     *                     block for an answer.
     * @return true if the sdk is ready, false otherwise.
     * @throws InterruptedException if this operation was interrupted.
     */
    public boolean isSDKReady(long milliseconds) throws InterruptedException {
        long end = System.currentTimeMillis() + milliseconds;
        long timeLeft = milliseconds;

        boolean splits = areSplitsReady(timeLeft);
        if (!splits) {
            return false;
        }

        timeLeft = end - System.currentTimeMillis();

        return areSegmentsReady(timeLeft);
    }

    public boolean isSDKReadyNow() {
        try {
            return isSDKReady(0);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Records that the SDK split initialization is done.
     * This operation is atomic and idempotent. Repeated invocations
     * will not have any impact on the state.
     */
    public void splitsAreReady() {
        long originalCount = _splitsAreReady.getCount();
        _splitsAreReady.countDown();
        if (originalCount > 0L) {
            _log.info("splits are ready");
        }
    }

    /**
     * Registers a segment that the SDK should download before it is ready.
     * This method should be called right after the first successful download
     * of split definitions.
     * <p/>
     * Note that if this method is called in subsequent fetches of splits,
     * it will return false; meaning any segments used in new splits
     * will not be able to block the SDK from being marked as complete.
     *
     * @param segmentName the segment to register
     * @return true if the segments were registered, false otherwise.
     * @throws InterruptedException
     */
    public boolean registerSegment(String segmentName) throws InterruptedException {
        if (segmentName == null || segmentName.isEmpty() || areSplitsReady(0L)) {
            return false;
        }

        _segmentsAreReady.putIfAbsent(segmentName, new CountDownLatch(1));
        _log.info("Registered segment: " + segmentName);
        return true;
    }

    /**
     * Records that the SDK segment initialization for this segment is done.
     * This operation is atomic and idempotent. Repeated invocations
     * will not have any impact on the state.
     */
    public void segmentIsReady(String segmentName) {
        CountDownLatch cdl = _segmentsAreReady.get(segmentName);
        if (cdl == null) {
            return;
        }

        long originalCount = cdl.getCount();

        cdl.countDown();

        if (originalCount > 0L) {
            _log.info(segmentName + " segment is ready");
        }
    }

    public boolean isSegmentRegistered(String segmentName) {
        return _segmentsAreReady.get(segmentName) != null;
    }

    /**
     * Returns true if the SDK is ready w.r.t segments. In other words, this method returns true if:
     * <ol>
     * <li>The SDK has fetched segment definitions the first time.</li>
     * </ol>
     * <p/>
     * This operation will block until the SDK is ready or 'milliseconds' have passed. If the milliseconds
     * are less than or equal to zero, the operation will not block and return immediately
     *
     * @param milliseconds time to wait for an answer. if the value is zero or negative, we will not
     *                     block for an answer.
     * @return true if the sdk is ready w.r.t splits, false otherwise.
     * @throws InterruptedException if this operation was interrupted.
     */
    public boolean areSegmentsReady(long milliseconds) throws InterruptedException {
        long end = System.currentTimeMillis() + milliseconds;
        long timeLeft = milliseconds;

        for (Map.Entry<String, CountDownLatch> entry : _segmentsAreReady.entrySet()) {
            String segmentName = entry.getKey();
            CountDownLatch cdl = entry.getValue();

            if (!cdl.await(timeLeft, TimeUnit.MILLISECONDS)) {
                _log.error(segmentName + " is not ready yet");
                return false;
            }

            timeLeft = end - System.currentTimeMillis();
        }

        return true;
    }

    /**
     * Returns true if the SDK is ready w.r.t splits. In other words, this method returns true if:
     * <ol>
     * <li>The SDK has fetched Split definitions the first time.</li>
     * </ol>
     * <p/>
     * This operation will block until the SDK is ready or 'milliseconds' have passed. If the milliseconds
     * are less than or equal to zero, the operation will not block and return immediately
     *
     * @param milliseconds time to wait for an answer. if the value is zero or negative, we will not
     *                     block for an answer.
     * @return true if the sdk is ready w.r.t splits, false otherwise.
     * @throws InterruptedException if this operation was interrupted.
     */
    public boolean areSplitsReady(long milliseconds) throws InterruptedException {
        return _splitsAreReady.await(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void sdkInternalReady() {
        _internalReady.countDown();
    }

    public void waitUntilInternalReady() throws InterruptedException {
        _internalReady.await();
    }
}
