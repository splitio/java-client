package io.split.client.impressions;

import io.split.client.dtos.DecoratedImpression;

import java.util.List;

public interface ImpressionsManager {

    public enum Mode {
        OPTIMIZED,
        DEBUG,
        NONE
    }

    void track(List<DecoratedImpression> decoratedImpressions);
    void start();
    void close();

    final class NoOpImpressionsManager implements ImpressionsManager {

        @Override
        public void track(List<DecoratedImpression> decoratedImpressions) { /* do nothing */ }

        @Override
        public void start(){
            /* do nothing */
        }

        @Override
        public void close() {
            /* do nothing */
        }
    }
}
