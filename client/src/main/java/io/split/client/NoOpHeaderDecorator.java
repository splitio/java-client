package io.split.client;

import io.split.client.CustomHeaderDecorator;
import io.split.client.dtos.RequestContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NoOpHeaderDecorator implements CustomHeaderDecorator {
    public NoOpHeaderDecorator() {
    }

    @Override
    public Map<String, List<String>> getHeaderOverrides(RequestContext context) {
        return new HashMap<>();
    }
}
