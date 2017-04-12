package io.split.engine.metrics;

/**
 * This interface is a briefer version of StatsD interface
 *
 * @author adil
 */
public interface Metrics {
    /**
     * Adjusts the specified counter by a given delta.
     * <p/>
     * <p>This method is is non-blocking and is guaranteed not to throw an exception.</p>
     *
     * @param counter the name of the counter to adjust
     * @param delta   the amount to adjust the counter by
     */
    void count(String counter, long delta);

    /**
     * Records an execution time in milliseconds for the specified named operation.
     * <p/>
     * <p>This method is non-blocking and is guaranteed not to throw an exception.</p>
     *
     * @param operation the name of the timed operation
     * @param timeInMs  the time in milliseconds
     */
    void time(String operation, long timeInMs);

    public static final class NoopMetrics implements Metrics {

        @Override
        public void count(String counter, long delta) {
            // noop
        }

        @Override
        public void time(String operation, long timeInMs) {
            // noop
        }
    }
}
