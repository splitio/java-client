package io.split.api.client;

import io.split.api.dtos.TrafficType;

import java.util.List;

public interface TrafficTypeClient {
    List<TrafficType> list();
}
