package io.split.api.client.api;

import io.split.api.client.TrafficTypeClient;
import io.split.api.dtos.TrafficType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApiTrafficTypeClient implements TrafficTypeClient {
    public List<TrafficType> list() {
        return new ArrayList<>();
    }

    @Override
    public TrafficType get(String name) {
        for (TrafficType trafficType : list()) {
            if (Objects.equals(name, trafficType.name())) {
                return trafficType;
            }
        }
        return null;
    }
}
