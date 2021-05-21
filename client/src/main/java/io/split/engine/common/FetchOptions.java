package io.split.engine.common;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class FetchOptions {

    public static final Long DEFAULT_TARGET_CHANGENUMBER = -1L;

    public static class Builder {

        public Builder() {}

        public Builder(FetchOptions opts) {
            _targetCN = opts._targetCN;
            _cacheControlHeaders = opts._cacheControlHeaders;
            _fastlyDebugHeader = opts._fastlyDebugHeader;
            _responseHeadersCallback = opts._responseHeadersCallback;
        }

        public Builder cacheControlHeaders(boolean on) {
            _cacheControlHeaders = on;
            return this;
        }

        public Builder fastlyDebugHeader(boolean on) {
            _fastlyDebugHeader = on;
            return this;
        }

        public Builder responseHeadersCallback(Function<Map<String, String>, Void> callback) {
            _responseHeadersCallback = callback;
            return this;
        }

        public Builder targetChangeNumber(long targetCN) {
            _targetCN = targetCN;
            return this;
        }

        public FetchOptions build() {
            return new FetchOptions(_cacheControlHeaders, _targetCN, _responseHeadersCallback, _fastlyDebugHeader);
        }

        private long _targetCN = DEFAULT_TARGET_CHANGENUMBER;
        private boolean _cacheControlHeaders = false;
        private boolean _fastlyDebugHeader = false;
        private Function<Map<String, String>, Void> _responseHeadersCallback = null;
    }

    public boolean cacheControlHeadersEnabled() {
        return _cacheControlHeaders;
    }

    public boolean fastlyDebugHeaderEnabled() {
        return _fastlyDebugHeader;
    }

    public long targetCN() { return _targetCN; }

    public boolean hasCustomCN() { return _targetCN != DEFAULT_TARGET_CHANGENUMBER; }

    public void handleResponseHeaders(Map<String, String> headers) {
        if (Objects.isNull(_responseHeadersCallback) || Objects.isNull(headers)) {
            return;
        }
        _responseHeadersCallback.apply(headers);
    }

    private FetchOptions(boolean cacheControlHeaders,
                         long targetCN,
                         Function<Map<String, String>, Void> responseHeadersCallback,
                         boolean fastlyDebugHeader) {
        _cacheControlHeaders = cacheControlHeaders;
        _targetCN = targetCN;
        _responseHeadersCallback = responseHeadersCallback;
        _fastlyDebugHeader = fastlyDebugHeader;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) return false;
        if (this == obj) return true;
        if (!(obj instanceof FetchOptions)) return false;

        FetchOptions other = (FetchOptions) obj;

        return Objects.equals(_cacheControlHeaders, other._cacheControlHeaders)
                && Objects.equals(_fastlyDebugHeader, other._fastlyDebugHeader)
                && Objects.equals(_responseHeadersCallback, other._responseHeadersCallback)
                && Objects.equals(_targetCN, other._targetCN);
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(_cacheControlHeaders, _fastlyDebugHeader, _responseHeadersCallback, _targetCN);
    }

    private final boolean _cacheControlHeaders;
    private final boolean _fastlyDebugHeader;
    private final long _targetCN;
    private final Function<Map<String, String>, Void> _responseHeadersCallback;
}
