package io.split.identify.dto;

import com.google.common.base.Preconditions;

public class Type {
    private String id;
    private String name;

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Builder builder() {
        return new Builder();
    }

    public Builder builder(Type other) {
        return new Builder(other);
    }

    private Type(Builder builder) {
        this.id = Preconditions.checkNotNull(builder.id);
        this.name = Preconditions.checkNotNull(builder.name);
    }

    public static class Builder {
        private String id;
        private String name;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        Builder() {
        }

        Builder(Type prototype) {
            id = prototype.id;
            name = prototype.name;
        }

        public Type build() {
            return new Type(this);
        }
    }
}
