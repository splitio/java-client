package io.split.httpmodules.okhttp;

import java.util.List;
import java.util.Map;

import io.split.client.RequestDecorator;
import io.split.client.dtos.RequestContext;

class OkHttpRequestDecorator {

    public static Map<String, List<String>> decorate(Map<String, List<String>> headers,
            RequestDecorator decorator) {
        return decorator.decorateHeaders(new RequestContext(headers)).headers();
    }
}
