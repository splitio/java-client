package io.split.client;

public interface ProxyRuntimeStorage
{
    /**
     * Get the additional headers needed for all http operations
     * @return HashMap of addition headers
     */
    String getJwtToken();
}
