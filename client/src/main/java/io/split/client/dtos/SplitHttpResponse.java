package io.split.client.dtos;

import java.util.List;

/**
 * A structure for returning http call results information
 */
public class SplitHttpResponse {
    private final Integer _statusCode;
    private final String _statusMessage;
    private final String _body;
    private final Header[] _responseHeaders;

    public static class Header {
        private String _name;
        private List<String> _values;

        public Header(String name, List<String> values) {
            _name = name;
            _values = values;
        }

        public String getName() {
            return _name;
        }

        public List<String> getValues() {
            return _values;
        }
    };

    public SplitHttpResponse(Integer statusCode, String statusMessage, String body, Header[] headers) {
        _statusCode = statusCode;
        _statusMessage = statusMessage;
        _body = body;
        _responseHeaders = headers;
    }

    public SplitHttpResponse(Integer statusCode, String statusMessage, String body, List<Header> headers) {
        _statusCode = statusCode;
        _statusMessage = statusMessage;
        _body = body;
        _responseHeaders = headers.toArray(new Header[0]);
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
