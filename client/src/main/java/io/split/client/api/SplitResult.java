package io.split.client.api;

import com.google.common.base.Objects;

public final class SplitResult {
    private final String _treatment;
    private final String _config;

    public SplitResult(String treatment, String config) {
        _treatment = treatment;
        _config = config;
    }

    public String treatment() {
        return _treatment;
    }

    public String config() {
        return _config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitResult)) return false;
        SplitResult that = (SplitResult) o;
        return Objects.equal(_treatment, that._treatment) &&
                Objects.equal(_config, that._config);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_treatment, _config);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(_treatment);
        bldr.append(", ");
        bldr.append(_config);
        return bldr.toString();
    }

}
