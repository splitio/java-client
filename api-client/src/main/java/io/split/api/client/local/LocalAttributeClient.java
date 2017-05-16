package io.split.api.client.local;

import io.split.api.client.AttributeClient;
import io.split.api.dtos.Attribute;
import io.split.api.dtos.TrafficType;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class LocalAttributeClient implements AttributeClient {
    @Override
    public List<Attribute> list(String trafficTypeId) {
        return new ArrayList<>();
    }

    @Override
    public List<Attribute> list(TrafficType trafficType) {
        return new ArrayList<>();
    }

    @Override
    public AttributeClient create(Attribute attribute) throws IllegalArgumentException {
        return this;
    }

    @Override
    public AttributeClient delete(String trafficTypeId, String propertyId) throws NoSuchElementException {
        return this;
    }

    @Override
    public AttributeClient delete(Attribute attribute) throws NoSuchElementException {
        return this;
    }
}
