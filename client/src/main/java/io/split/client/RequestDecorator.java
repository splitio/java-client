package io.split.client;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import java.util.*;

interface UserCustomHeaderDecorator
{
    Map<String, String> getHeaderOverrides();
}

class NoOpHeaderDecorator implements  UserCustomHeaderDecorator {
    public NoOpHeaderDecorator() {}
    @Override
    public Map<String, String> getHeaderOverrides() {
        return new HashMap<String, String>();
    }
}

public final class RequestDecorator {
    UserCustomHeaderDecorator _headerDecorator;

    private static final Set<String> forbiddenHeaders = new HashSet<>(Arrays.asList(
            "SplitSDKVersion",
            "SplitMachineIp",
            "SplitMachineName",
            "SplitImpressionsMode",
            "Host",
            "Referrer",
            "Content-Type",
            "Content-Length",
            "Content-Encoding",
            "Accept",
            "Keep-Alive",
            "X-Fastly-Debug"
    ));

    public RequestDecorator(UserCustomHeaderDecorator headerDecorator) {
        _headerDecorator = (headerDecorator == null)
                ? new NoOpHeaderDecorator()
                : headerDecorator;
    }

    public HttpUriRequestBase decorateHeaders(HttpUriRequestBase request) {
        try {
            Map<String, String> headers = _headerDecorator.getHeaderOverrides();
            for (Map.Entry entry : headers.entrySet()) {
                if (isHeaderAllowed(entry.getKey().toString())) {
                    request.addHeader(entry.getKey().toString(), entry.getValue());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Problem adding custom headers to request decorator: %s", e), e);
        }

        return request;
    }

    private boolean isHeaderAllowed(String headerName) {
        return !forbiddenHeaders.contains(headerName);
    }
}
