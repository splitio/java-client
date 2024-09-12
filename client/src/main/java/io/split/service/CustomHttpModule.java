package io.split.service;

import io.split.client.RequestDecorator;
import io.split.client.utils.SDKMetadata;

import java.io.IOException;

public interface CustomHttpModule {
    public SplitHttpClient createClient(String apiToken, SDKMetadata sdkMetadata, RequestDecorator decorator)
            throws IOException;
}
