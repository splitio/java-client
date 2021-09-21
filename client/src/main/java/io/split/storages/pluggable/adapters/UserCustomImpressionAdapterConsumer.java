package io.split.storages.pluggable.adapters;

import io.split.client.dtos.KeyImpression;
import io.split.client.impressions.ImpressionsStorageConsumer;

import java.util.ArrayList;
import java.util.List;

public class UserCustomImpressionAdapterConsumer implements ImpressionsStorageConsumer {
    @Override
    public List<KeyImpression> pop(int count) {
        //No-Op
        return new ArrayList<>();
    }

    @Override
    public List<KeyImpression> pop() {
        //No-Op
        return new ArrayList<>();
    }

    @Override
    public boolean isFull() {
        //No-Op
        return false;
    }
}
