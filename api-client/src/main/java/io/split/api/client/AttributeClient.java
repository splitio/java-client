package io.split.api.client;

import io.split.api.dtos.Attribute;
import io.split.api.dtos.TrafficType;

import java.util.List;
import java.util.NoSuchElementException;

public interface AttributeClient {
    List<Attribute> get(String trafficTypeId);

    List<Attribute> get(TrafficType trafficType);

    AttributeClient create(Attribute attribute) throws IllegalArgumentException;

    AttributeClient delete(String trafficTypeId, String propertyId) throws NoSuchElementException;

    AttributeClient delete(Attribute attribute) throws NoSuchElementException;
}
