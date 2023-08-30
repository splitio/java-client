package io.split.client.utils;

import io.split.client.exceptions.InputStreamProviderException;

import java.io.InputStream;

public class InputStreamProviderImp implements InputStreamProvider {
    private InputStream _inputStream;

    public InputStreamProviderImp(InputStream inputStream){
        _inputStream = inputStream;
    }

    @Override
    public InputStream get() throws InputStreamProviderException {
        return _inputStream;
    }
}