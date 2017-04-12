package io.split.client;

import io.split.engine.experiments.SplitFetcher;
import io.split.client.impressions.ImpressionListener;
import io.split.engine.metrics.Metrics;

/**
 * Created by adilaijaz on 7/15/16.
 */
public class SplitFactoryImpl implements SplitFactory {

    private final SplitClient _client;
    private final SplitManager _manager;

    public SplitFactoryImpl(SplitFetcher fetcher, ImpressionListener impressionListener, Metrics metrics, SplitClientConfig config) {
        _client = new SplitClientImpl(fetcher, impressionListener, metrics, config);
        _manager = new SplitManagerImpl(fetcher);

    }

    public SplitClient client() {
        return _client;
    }

    public SplitManager manager() {
        return _manager;
    }


}
