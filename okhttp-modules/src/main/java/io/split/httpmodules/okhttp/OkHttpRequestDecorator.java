package io.split.httpmodules.okhttp;

import java.util.List;
import java.util.Map;

import io.split.client.RequestDecorator;
import io.split.client.dtos.RequestContext;

class OkHttpRequestDecorator {

    public static okhttp3.Request.Builder decorate(Map<String, List<String>> headers, okhttp3.Request.Builder b,
            RequestDecorator decorator) {
        headers = decorator.decorateHeaders(new RequestContext(headers)).headers();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            for (String headerValue : e.getValue()) {
                b.addHeader(e.getKey(), headerValue);
            }
        }
        return b;
    }
}
