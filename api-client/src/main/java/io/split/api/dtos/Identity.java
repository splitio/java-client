package io.split.api.dtos;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Identity {
    private String trafficTypeId;
    private String environmentId;
    private String key;
    private Map<String, String> values;

    public String trafficTypeId() {
        return trafficTypeId;
    }

    public String environmentId() {
        return environmentId;
    }

    public String key() {
        return key;
    }

    public Map<String, String> values() {
        return values;
    }

    public Builder builder() {
        return new Builder();
    }

    public Builder builder(Identity other) {
        return new Builder(other);
    }

    private Identity(Builder builder) {
        this.trafficTypeId = builder.trafficTypeId;
        this.environmentId = builder.environmentId;
        this.key = builder.key;
        this.values = builder.values;
    }

    public static class Builder {
        private String trafficTypeId;
        private String environmentId;
        private String key;
        private Map<String, String> values;

        public Builder trafficTypeId(String trafficTypeId) {
            this.trafficTypeId = trafficTypeId;
            return this;
        }

        public Builder trafficType(TrafficType trafficType) {
            this.trafficTypeId = trafficType.id();
            return this;
        }

        public Builder environmentId(String environmentId) {
            this.environmentId = environmentId;
            return this;
        }

        public Builder environment(Environment environment) {
            this.environmentId = environment.id();
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder values(Map<String, String> values) {
            this.values = values;
            return this;
        }

        public Builder addValue(String name, boolean value) {
            this.values.put(name, Boolean.toString(value));
            return this;
        }

        public Builder addValue(String name, int value) {
            this.values.put(name, Integer.toString(value));
            return this;
        }

        public Builder addValue(String name, long value) {
            this.values.put(name, Long.toString(value));
            return this;
        }

        public Builder addValue(String name, float value) {
            this.values.put(name, Float.toString(value));
            return this;
        }

        public Builder addValue(String name, double value) {
            this.values.put(name, Double.toString(value));
            return this;
        }

        public Builder addValue(String name, Date value) {
            this.values.put(name, Long.toString(value.getTime()));
            return this;
        }

        public Builder addValue(String name, String value) {
            this.values.put(name, value);
            return this;
        }

        Builder() {
            values = new HashMap<>();
        }

        Builder(Identity prototype) {
            trafficTypeId = prototype.trafficTypeId;
            environmentId = prototype.environmentId;
            key = prototype.key;
            values = prototype.values;
        }

        public Identity build() {
            return new Identity(this);
        }
    }
}
