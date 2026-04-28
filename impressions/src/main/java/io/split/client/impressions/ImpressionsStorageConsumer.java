package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;

import java.util.List;

public interface ImpressionsStorageConsumer {
    List<KeyImpression> pop(int count);
    List<KeyImpression> pop();
    boolean isFull();
}
