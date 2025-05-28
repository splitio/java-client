package io.split.engine.common;

import java.util.Objects;

public class FetchOptions {

    public static final Long DEFAULT_TARGET_CHANGENUMBER = -1L;

    public static class Builder {

        public Builder() {}

        public Builder(FetchOptions opts) {
            _targetCN = opts._targetCN;
            _targetCnRBS = opts._targetCnRBS;
            _cacheControlHeaders = opts._cacheControlHeaders;
            _flagSetsFilter = opts._flagSetsFilter;
        }

        public Builder cacheControlHeaders(boolean on) {
            _cacheControlHeaders = on;
            return this;
        }

        public Builder targetChangeNumber(long targetCN) {
            _targetCN = targetCN;
            return this;
        }

        public Builder targetChangeNumberRBS(long targetCnRBS) {
            _targetCnRBS = targetCnRBS;
            return this;
        }

        public Builder flagSetsFilter(String flagSetsFilter) {
            _flagSetsFilter = flagSetsFilter;
            return this;
        }

        public FetchOptions build() {
            return new FetchOptions(_cacheControlHeaders, _targetCN, _targetCnRBS, _flagSetsFilter);
        }

        private long _targetCN = DEFAULT_TARGET_CHANGENUMBER;
        private long _targetCnRBS = DEFAULT_TARGET_CHANGENUMBER;
        private boolean _cacheControlHeaders = false;
        private String _flagSetsFilter = "";
    }

    public boolean cacheControlHeadersEnabled() {
        return _cacheControlHeaders;
    }

    public long targetCN() { return _targetCN; }

    public long targetCnRBS() { return _targetCnRBS; }

    public boolean hasCustomCN() { return _targetCN != DEFAULT_TARGET_CHANGENUMBER; }

    public String flagSetsFilter() {
        return _flagSetsFilter;
    }

    private FetchOptions(boolean cacheControlHeaders,
                         long targetCN,
                         long targetCnRBS,
                         String flagSetsFilter) {
        _cacheControlHeaders = cacheControlHeaders;
        _targetCN = targetCN;
        _targetCnRBS = targetCnRBS;
        _flagSetsFilter = flagSetsFilter;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) return false;
        if (this == obj) return true;
        if (!(obj instanceof FetchOptions)) return false;

        FetchOptions other = (FetchOptions) obj;

        return Objects.equals(_cacheControlHeaders, other._cacheControlHeaders)
                && Objects.equals(_targetCN, other._targetCN)
                && Objects.equals(_targetCnRBS, other._targetCnRBS)
                && Objects.equals(_flagSetsFilter, other._flagSetsFilter);
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(_cacheControlHeaders,
                _targetCN, _flagSetsFilter);
    }

    private final boolean _cacheControlHeaders;
    private final long _targetCN;
    private final long _targetCnRBS;
    private final String _flagSetsFilter;
}