package io.split.client.impressions;

import io.split.client.dtos.UniqueKeys;

public interface UniqueKeysSender {
    void send(UniqueKeys uniqueKeys);
}
