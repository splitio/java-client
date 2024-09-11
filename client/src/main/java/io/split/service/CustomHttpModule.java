package io.split.service;

import io.split.client.RequestDecorator;
import io.split.client.utils.SDKMetadata;
import io.split.service.SplitHttpClient;

import java.io.IOException;

public interface CustomHttpModule {
    public SplitHttpClient createClient(String apiToken, SDKMetadata sdkMetadata, RequestDecorator requestDecorator) throws IOException;


}
