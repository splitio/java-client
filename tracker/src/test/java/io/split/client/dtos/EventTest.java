package io.split.client.dtos;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class EventTest {

    @Test
    public void equalsReturnsTrueForSameObject() {
        Event event = new Event();
        assertTrue(event.equals(event));
    }

    @Test
    public void equalsReturnsFalseForNull() {
        Event event = new Event();
        assertFalse(event.equals(null));
    }

    @Test
    public void equalsReturnsFalseForDifferentClass() {
        Event event = new Event();
        assertFalse(event.equals("not an event"));
    }

    @Test
    public void equalsReturnsTrueForIdenticalEvents() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 1000L;

        assertTrue(event1.equals(event2));
    }

    @Test
    public void equalsReturnsFalseForDifferentEventTypeId() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "view";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 1000L;

        assertFalse(event1.equals(event2));
    }

    @Test
    public void equalsReturnsFalseForDifferentTrafficTypeName() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "account";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 1000L;

        assertFalse(event1.equals(event2));
    }

    @Test
    public void equalsReturnsFalseForDifferentKey() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user456";
        event2.value = 100.0;
        event2.timestamp = 1000L;

        assertFalse(event1.equals(event2));
    }

    @Test
    public void equalsReturnsFalseForDifferentValue() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 200.0;
        event2.timestamp = 1000L;

        assertFalse(event1.equals(event2));
    }

    @Test
    public void equalsReturnsFalseForDifferentTimestamp() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 2000L;

        assertFalse(event1.equals(event2));
    }

    @Test
    public void equalsIgnoresPropertiesField() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;
        event1.properties = new HashMap<>();
        event1.properties.put("color", "red");

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 1000L;
        event2.properties = new HashMap<>();
        event2.properties.put("size", "large");

        assertTrue(event1.equals(event2));
    }

    @Test
    public void equalsIsSelfConsistent() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 1000L;

        assertTrue(event1.equals(event2));
        assertTrue(event1.equals(event2));
    }

    @Test
    public void equalsIsSymmetric() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 1000L;

        assertTrue(event1.equals(event2));
        assertTrue(event2.equals(event1));
    }

    @Test
    public void equalsIsTransitive() {
        Event event1 = new Event();
        event1.eventTypeId = "purchase";
        event1.trafficTypeName = "user";
        event1.key = "user123";
        event1.value = 100.0;
        event1.timestamp = 1000L;

        Event event2 = new Event();
        event2.eventTypeId = "purchase";
        event2.trafficTypeName = "user";
        event2.key = "user123";
        event2.value = 100.0;
        event2.timestamp = 1000L;

        Event event3 = new Event();
        event3.eventTypeId = "purchase";
        event3.trafficTypeName = "user";
        event3.key = "user123";
        event3.value = 100.0;
        event3.timestamp = 1000L;

        assertTrue(event1.equals(event2));
        assertTrue(event2.equals(event3));
        assertTrue(event1.equals(event3));
    }

}
