package io.split.client.dtos;

import java.util.Map;
import org.apache.hc.core5.http.Header;
/**
 * A structure for returning http call results information
 */
public class SplitHttpResponse {
    private final Integer _statusCode;
    private final String _statusMessage;
    private final String _body;
    private final Header[] _responseHeaders;

    public SplitHttpResponse(Integer statusCode, String statusMessage, String body, Header[] headers) {
        _statusCode = statusCode;
        _statusMessage = statusMessage;
        _body = body;
        _responseHeaders = headers;
    }
    public Integer statusCode() {
        return _statusCode;
    }
    public String statusMessage() {
        return _statusMessage;
    }

    public String body() {
        return _body;
    }

    public Header[] responseHeaders() {
        return _responseHeaders;
    }
}
