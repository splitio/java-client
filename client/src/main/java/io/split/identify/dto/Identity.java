package io.split.identify.dto;

import com.google.common.base.Preconditions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Identity {
    private String typeId;
    private String environmentId;
    private String key;
    private Map<String, Value> values;

    public String typeId() {
        return typeId;
    }

    public String environmentId() {
        return environmentId;
    }

    public String key() {
        return key;
    }

    public Map<String, Value> values() {
        return values;
    }

    public Builder builder() {
        return new Builder();
    }

    public Builder builder(Identity other) {
        return new Builder(other);
    }

    private Identity(Builder builder) {
        this.typeId = Preconditions.checkNotNull(builder.typeId);
        this.environmentId = Preconditions.checkNotNull(builder.environmentId);
        this.key = Preconditions.checkNotNull(builder.key);
        this.values = Preconditions.checkNotNull(builder.values);
    }

    public static class Builder {
        private String typeId;
        private String environmentId;
        private String key;
        private Map<String, Value> values;

        public Builder typeId(String typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder type(Type type) {
            this.typeId = type.id();
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

        public Builder values(Map<String, Value> values) {
            this.values = values;
            return this;
        }

        public Builder addValue(String name, boolean value) {
            this.values.put(name, new Value(value));
            return this;
        }

        public Builder addValue(String name, int value) {
            this.values.put(name, new Value(value));
            return this;
        }

        public Builder addValue(String name, long value) {
            this.values.put(name, new Value(value));
            return this;
        }

        public Builder addValue(String name, float value) {
            this.values.put(name, new Value(value));
            return this;
        }

        public Builder addValue(String name, double value) {
            this.values.put(name, new Value(value));
            return this;
        }

        public Builder addValue(String name, Date value) {
            this.values.put(name, new Value(value));
            return this;
        }

        public Builder addValue(String name, String value) {
            this.values.put(name, new Value(value));
            return this;
        }

        Builder() {
            values = new HashMap<>();
        }

        Builder(Identity prototype) {
            typeId = prototype.typeId;
            environmentId = prototype.environmentId;
            key = prototype.key;
            values = prototype.values;
        }

        public Identity build() {
            return new Identity(this);
        }
    }
}
