package io.split.client.dtos;

import java.util.Map;
import org.apache.hc.core5.http.Header;
/**
 * A structure for returning http call results information
 */
public final class SplitHttpResponse {
    public Integer statusCode;
    public String statusMessage;
    public String body;
    public Header[] responseHeaders;
}
