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

    private final CountDownLatch _internalReady = new CountDownLatch(1);

    /**
     * Returns true if the SDK is ready. The SDK is ready when:
     * <ol>
     * <li>It has fetched Feature flag definitions the first time.</li>
     * <li>It has downloaded segment memberships for segments in use in the initial Feature flag definitions</li>
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
    public boolean waitUntilInternalReady(long milliseconds) throws InterruptedException {
        return _internalReady.await(milliseconds, TimeUnit.MILLISECONDS);
    }

    public boolean isSDKReady() {
        return _internalReady.getCount() == 0;
    }

    public void sdkInternalReady() {
        _internalReady.countDown();
    }
}
