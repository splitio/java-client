package io.split.client;

import io.split.client.dtos.RequestContext;


import java.util.HashSet;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

    public RequestContext decorateHeaders(RequestContext request) {
        try {
            return new RequestContext(_headerDecorator.getHeaderOverrides(request)
                    .entrySet()
                    .stream()
                    .filter(e -> !forbiddenHeaders.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Problem adding custom headers to request decorator: %s", e), e);
        }
    }
}
