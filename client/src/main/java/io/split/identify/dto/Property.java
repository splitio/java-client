package io.split.identify.dto;

import com.google.common.base.Preconditions;

public class Property {
    private String id;
    private String typeId;
    private String name;
    private String dataType;
    private String description;

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String dataType() {
        return dataType;
    }

    public String description() {
        return description;
    }

    public String typeId() {
        return typeId;
    }

    public Builder builder() {
        return new Builder();
    }

    public Builder builder(Property other) {
        return new Builder(other);
    }

    private Property(Builder builder) {
        this.typeId = Preconditions.checkNotNull(builder.typeId);
        this.id = Preconditions.checkNotNull(builder.id);
        this.name = Preconditions.checkNotNull(builder.name);
        this.dataType = Preconditions.checkNotNull(builder.dataType);
        this.description = Preconditions.checkNotNull(builder.description);
    }

    public static class Builder {
        private String typeId;
        private String id;
        private String name;
        private String dataType;
        private String description;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(Type type) {
            this.typeId = type.id();
            return this;
        }

        public Builder typeId(String typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        Builder() {

        }

        Builder(Property prototype) {
            typeId = prototype.typeId;
            id = prototype.id;
            name = prototype.name;
            dataType = prototype.dataType;
            description = prototype.description;
        }

        public Property build() {
            return new Property(this);
        }
    }
}
