package io.split.client.dtos;

import java.util.Map;
import java.util.List;
/**
 * A structure returning a context for RequestDecorator class
 */

public class RequestContext
{
    private final Map<String, List<String>> _headers;

    public RequestContext(Map<String, List<String>> headers) {
        _headers = headers;
    }

    public Map<String, List<String>> headers() {
        return _headers;
    }
}
