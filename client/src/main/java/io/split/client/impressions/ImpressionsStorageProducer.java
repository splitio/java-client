package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;

import java.util.List;

public interface ImpressionsStorageProducer {
    boolean put(KeyImpression imps);
    boolean put(List<KeyImpression> imps);
}
