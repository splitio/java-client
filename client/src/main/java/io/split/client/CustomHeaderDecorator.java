package io.split.client;

import io.split.client.dtos.RequestContext;

import java.util.Map;
import java.util.List;

public interface CustomHeaderDecorator
{
    /**
     * Get the additional headers needed for all http operations
     * @return HashMap of addition headers
     */
    Map<String, List<String>> getHeaderOverrides(RequestContext context);
}
