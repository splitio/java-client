package io.split.client.impressions;

import java.util.List;

/**
 * A listener for Impressions generated each time getTreatment is called.
 *
 * @author adil
 */
public interface ImpressionListener {

    /**
     * Log this impression to the listener. This method MUST NOT throw any exception
     *
     * @param impression
     */
    void log(Impression impression);

    /**
     * MUST NOT throw any exceptions
     */
    void close();

    final class NoopImpressionListener implements ImpressionListener {
        @Override
        public void log(Impression impression) {
            // noop
        }

        @Override
        public void close() {
            // noop
        }
    }

    final class FederatedImpressionListener implements ImpressionListener {
        private List<ImpressionListener> _delegates;

        public FederatedImpressionListener(List<ImpressionListener> delegates) {
            _delegates = delegates;
        }

        @Override
        public void log(Impression impression) {
            for (ImpressionListener listener : _delegates) {
                listener.log(impression);
            }
        }

        @Override
        public void close() {
            for (ImpressionListener listener : _delegates) {
                listener.close();
            }
        }
    }

}
