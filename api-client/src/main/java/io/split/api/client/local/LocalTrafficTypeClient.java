package io.split.api.client.local;

import io.split.api.client.TrafficTypeClient;
import io.split.api.dtos.TrafficType;

import java.util.ArrayList;
import java.util.List;

public class LocalTrafficTypeClient implements TrafficTypeClient {
    @Override
    public List<TrafficType> list() {
        return new ArrayList<>();
    }
}
