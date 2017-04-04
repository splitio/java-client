package io.split.client.impressions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around an ImpressionListener provided by the customer. The purpose
 * of the wrapper is to protect the SplitClient from any slow down happening due
 * to the client's ImpressionListener.
 *
 * @author adil
 */
public class AsynchronousImpressionListener implements ImpressionListener {

    private static final Logger _log = LoggerFactory.getLogger(AsynchronousImpressionListener.class);

    private final ImpressionListener _delegate;
    private final ExecutorService _executor;

    public static AsynchronousImpressionListener build(ImpressionListener delegate, int capacity) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("impression-listener-wrapper-%d")
                .build();

        ExecutorService executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(capacity), threadFactory);

        return new AsynchronousImpressionListener(delegate, executor);
    }

    public AsynchronousImpressionListener(ImpressionListener delegate, ExecutorService executor) {
        _delegate = delegate;
        _executor = executor;
    }


    @Override
    public void log(final Impression impression) {
        try {
            _executor.execute(new Runnable() {
                @Override
                public void run() {
                    _delegate.log(impression);
                }
            });
        }
        catch (Exception e) {
            _log.warn("Unable to send impression to impression listener", e);
        }
    }

    @Override
    public void close() {
        try {
            _executor.shutdown();
            _delegate.close();
        } catch (Exception e) {
            _log.warn("Unable to close AsynchronousImpressionListener", e);
        }
    }
}
