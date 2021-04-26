package io.split.engine.common;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class FetchOptions {
    
    public static class Builder {
        public Builder() {}

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

        public FetchOptions build() {
            return new FetchOptions(_cacheControlHeaders, _responseHeadersCallback, _fastlyDebugHeader);
        }

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

    public void handleResponseHeaders(Map<String, String> headers) {
        if (Objects.isNull(_responseHeadersCallback) || Objects.isNull(headers)) {
            return;
        }
        _responseHeadersCallback.apply(headers);
    }

    private FetchOptions(boolean cacheControlHeaders, Function<Map<String, String>, Void> responseHeadersCallback,
                         boolean fastlyDebugHeader) {
        _cacheControlHeaders = cacheControlHeaders;
        _responseHeadersCallback = responseHeadersCallback;
        _fastlyDebugHeader = fastlyDebugHeader;
    }

    private final boolean _cacheControlHeaders;
    private final boolean _fastlyDebugHeader;
    private final Function<Map<String, String>, Void> _responseHeadersCallback;
}
