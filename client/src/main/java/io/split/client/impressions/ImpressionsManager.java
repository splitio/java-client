package io.split.client.impressions;

import java.util.List;

public interface ImpressionsManager {

    public enum Mode {
        OPTIMIZED,
        DEBUG
    }

    void track(List<Impression> impressions);
    void start();

    final class NoOpImpressionsManager implements ImpressionsManager {

        @Override
        public void track(List<Impression> impressions) { /* do nothing */ }

        @Override
        public void start(){
            /* do nothing */
        }
    }
}
