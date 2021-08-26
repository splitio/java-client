package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;

import java.util.List;

public interface ImpressionsStorageProducer {
    boolean put(KeyImpression imps); //TODO get rid when getTreatmens is implemented.
    boolean put(List<KeyImpression> imps); // TODO start using it for all impression operations and split impressionlistener vs storage producer.
}
