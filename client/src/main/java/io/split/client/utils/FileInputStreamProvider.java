package io.split.client.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileInputStreamProvider implements InputStreamProvider {

    private final String _fileName;

    public FileInputStreamProvider(String fileName) {
        _fileName = fileName;
    }

    @Override
    public InputStream get() throws FileNotFoundException {
        return new FileInputStream(_fileName);
    }
}
