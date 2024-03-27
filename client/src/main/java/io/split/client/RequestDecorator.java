package io.split.client;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.List;

class NoOpHeaderDecorator implements  UserCustomHeaderDecorator {
    public NoOpHeaderDecorator() {}
    @Override
    public Map<String, List<String>> getHeaderOverrides() {
        return new HashMap<>();
    }
}

public final class RequestDecorator {
    UserCustomHeaderDecorator _headerDecorator;

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
            "x-fastly-debug"
    ));

    public RequestDecorator(UserCustomHeaderDecorator headerDecorator) {
        _headerDecorator = (headerDecorator == null)
                ? new NoOpHeaderDecorator()
                : headerDecorator;
    }

    public HttpUriRequestBase decorateHeaders(HttpUriRequestBase request) {
        try {
            Map<String, List<String>> headers = _headerDecorator.getHeaderOverrides();
            for (Map.Entry entry : headers.entrySet()) {
                if (isHeaderAllowed(entry.getKey().toString())) {
                    List<String> values = (List<String>) entry.getValue();
                    for (int i = 0; i < values.size(); i++) {
                        if (i == 0) {
                            request.setHeader(entry.getKey().toString(), values.get(i));
                        } else {
                            request.addHeader(entry.getKey().toString(), values.get(i));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Problem adding custom headers to request decorator: %s", e), e);
        }

        return request;
    }

    private boolean isHeaderAllowed(String headerName) {
        return !forbiddenHeaders.contains(headerName.toLowerCase());
    }
}
