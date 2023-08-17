package io.split.client.utils;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class InputStreamProviderImp implements InputStreamProvider {
    private InputStream _inputStream;

    public InputStreamProviderImp(InputStream inputStream){
        _inputStream = inputStream;
    }

    @Override
    public InputStream get() throws FileNotFoundException {
        return _inputStream;
    }
}