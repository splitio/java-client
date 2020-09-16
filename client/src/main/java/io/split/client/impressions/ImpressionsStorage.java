package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;

public interface ImpressionsStorage extends ImpressionsStorageConsumer, ImpressionsStorageProducer {

    boolean put(KeyImpression imp);
}
