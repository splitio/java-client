package io.split.client.dtos;

public interface BasicCredentialsProvider extends ProxyCredentialsProvider
{
    String getUsername();
    String getPassword();
}
