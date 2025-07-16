package io.split.client.dtos;

public interface BearerCredentialsProvider extends ProxyCredentialsProvider
{
    String getToken();
}
