package io.split.client.utils;

import io.split.client.exceptions.InputStreamProviderException;

import java.io.InputStream;

public interface InputStreamProvider {

    InputStream get() throws InputStreamProviderException;
}
