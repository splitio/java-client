package io.split.api.dtos;

public class Attribute {
    private String id;
    private String trafficTypeId;
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

    public String trafficTypeId() {
        return trafficTypeId;
    }

    public Builder builder() {
        return new Builder();
    }

    public Builder builder(Attribute other) {
        return new Builder(other);
    }

    private Attribute(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.dataType = builder.dataType;
        this.description = builder.description;
        this.trafficTypeId = builder.trafficTypeId;
    }

    public static class Builder {
        private String id;
        private String name;
        private String dataType;
        private String description;
        private String trafficTypeId;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder trafficType(TrafficType trafficType) {
            this.trafficTypeId = trafficType.id();
            return this;
        }

        public Builder trafficTypeId(String trafficTypeId) {
            this.trafficTypeId = trafficTypeId;
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

        Builder(Attribute prototype) {
            trafficTypeId = prototype.trafficTypeId;
            id = prototype.id;
            name = prototype.name;
            dataType = prototype.dataType;
            description = prototype.description;
        }

        public Attribute build() {
            return new Attribute(this);
        }
    }
}
