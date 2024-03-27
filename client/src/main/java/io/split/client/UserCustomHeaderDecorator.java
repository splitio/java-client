package io.split.client;

import java.util.Map;
import java.util.List;

public interface UserCustomHeaderDecorator
{
    /**
     * Get the additional headers needed for all http operations
     * @return HashMap of addition headers
     */
    Map<String, List<String>> getHeaderOverrides();
}
