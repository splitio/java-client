package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;

import java.util.List;

public interface ImpressionsStorageProducer {
    long put(List<KeyImpression> imps);
}
