package io.split.engine.common;

import java.util.*;
import java.util.stream.Collectors;

public class FastlyHeadersCaptor {

    private static final Set<String> HEADERS_TO_CAPTURE = new HashSet<>(Arrays.asList(
            "Fastly-Debug-Path",
            "Fastly-Debug-TTL",
            "Fastly-Debug-Digest",
            "X-Served-By",
            "X-Cache",
            "X-Cache-Hits",
            "X-Timer",
            "Surrogate-Key",
            "ETag",
            "Cache-Control",
            "X-Request-ID",
           "Last-Modified"
    ));

    private final List<Map<String, String>> _headers = new ArrayList<>();

    public Void handle(Map<String, String> responseHeaders) {
        _headers.add(responseHeaders.entrySet().stream()
                .filter(e -> HEADERS_TO_CAPTURE.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return null;
    }

    public List<Map<String, String>> get() {
        return _headers;
    }
}