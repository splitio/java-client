package io.split.client.impressions;

public interface ImpressionsManager {
    void track(Impression impression);

    final class NoOpImpressionsManager implements ImpressionsManager {

        @Override
        public void track(Impression impression) { /* do nothing */ }
    }
}
