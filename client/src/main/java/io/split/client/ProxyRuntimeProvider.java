package io.split.client;

public interface ProxyRuntimeProvider
{
    /**
     * Get the additional headers needed for all http operations
     * @return HashMap of addition headers
     */
    String getJwtToken();
}
