package io.split.api.client.api;

import io.split.api.client.TrafficTypeClient;
import io.split.api.dtos.TrafficType;

import java.util.ArrayList;
import java.util.List;

public class ApiTrafficTypeClient implements TrafficTypeClient {

    public List<TrafficType> list() {
        return new ArrayList<>();
    }
}
