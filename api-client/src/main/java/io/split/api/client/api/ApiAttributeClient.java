package io.split.api.client.api;

import io.split.api.client.AttributeClient;
import io.split.api.dtos.Attribute;
import io.split.api.dtos.TrafficType;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ApiAttributeClient implements AttributeClient {
    public List<Attribute> get(String trafficTypeId) {
        return new ArrayList<>();
    }

    public List<Attribute> get(TrafficType trafficType) {
        return get(trafficType.id());
    }

    public ApiAttributeClient create(Attribute attribute) throws IllegalArgumentException {
        return this;
    }

    public ApiAttributeClient delete(String trafficTypeId, String propertyId) throws NoSuchElementException {
        return this;
    }

    public ApiAttributeClient delete(Attribute attribute) throws NoSuchElementException {
        delete(attribute.trafficTypeId(), attribute.id());
        return this;
    }
}
