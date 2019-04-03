package io.split.client.api;

import com.google.common.base.Objects;

public final class SplitResult {
    private final String _treatment;
    private final String _configurations;

    public SplitResult(String treatment, String configurations) {
        _treatment = treatment;
        _configurations = configurations;
    }

    public String treatment() {
        return _treatment;
    }

    public String configurations() {
        return _configurations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitResult)) return false;
        SplitResult that = (SplitResult) o;
        return Objects.equal(_treatment, that._treatment) &&
                Objects.equal(_configurations, that._configurations);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_treatment, _configurations);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(_treatment);
        bldr.append(", ");
        bldr.append(_configurations);
        return bldr.toString();
    }

}
