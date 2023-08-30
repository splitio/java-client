package io.split.client.utils;

import io.split.client.exceptions.InputStreamProviderException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileInputStreamProvider implements InputStreamProvider {

    private final String _fileName;

    public FileInputStreamProvider(String fileName) {
        _fileName = fileName;
    }

    @Override
    public InputStream get() throws InputStreamProviderException {
        try {
            return new FileInputStream(_fileName);
        } catch (FileNotFoundException f) {
            throw new InputStreamProviderException(_fileName, f.getMessage());
        }
    }
}