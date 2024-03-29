package io.split.client;

import io.split.client.dtos.RequestContext;

//`import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Header;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;

class NoOpHeaderDecorator implements CustomHeaderDecorator {
    public NoOpHeaderDecorator() {
    }

    @Override
    public Map<String, List<String>> getHeaderOverrides(RequestContext context) {
        return new HashMap<>();
    }
}

public final class RequestDecorator {
    CustomHeaderDecorator _headerDecorator;

    private static final Set<String> forbiddenHeaders = new HashSet<>(Arrays.asList(
            "splitsdkversion",
            "splitmachineip",
            "splitmachinename",
            "splitimpressionsmode",
            "host",
            "referrer",
            "content-type",
            "content-length",
            "content-encoding",
            "accept",
            "keep-alive",
            "x-fastly-debug"));

    public RequestDecorator(CustomHeaderDecorator headerDecorator) {
        _headerDecorator = (headerDecorator == null)
                ? new NoOpHeaderDecorator()
                : headerDecorator;
    }

    public HttpRequest decorateHeaders(HttpRequest request) {
        try {
            Map<String, List<String>> headers = _headerDecorator
                    .getHeaderOverrides(new RequestContext(convertToMap(request.getHeaders())));
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (isHeaderAllowed(entry.getKey())) {
                    List<String> values = entry.getValue();
                    for (int i = 0; i < values.size(); i++) {
                        if (i == 0) {
                            request.setHeader(entry.getKey(), values.get(i));
                        } else {
                            request.addHeader(entry.getKey(), values.get(i));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Problem adding custom headers to request decorator: %s", e), e);
        }

        return request;
    }

    private boolean isHeaderAllowed(String headerName) {
        return !forbiddenHeaders.contains(headerName.toLowerCase());
    }

    private Map<String, List<String>> convertToMap(Header[] to_convert) {
        Map<String, List<String>> to_return = new HashMap<String, List<String>>();
        for (Integer i = 0; i < to_convert.length; i++) {
            if (!to_return.containsKey(to_convert[i].getName())) {
                to_return.put(to_convert[i].getName(), new ArrayList<String>());
            }
            to_return.get(to_convert[i].getName()).add(to_convert[i].getValue());
        }
        return to_return;
    }
}
