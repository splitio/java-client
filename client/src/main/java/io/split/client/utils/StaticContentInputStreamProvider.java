package io.split.client.utils;

import io.split.client.exceptions.InputStreamProviderException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class StaticContentInputStreamProvider implements InputStreamProvider {

    private final String _streamContents;

    public StaticContentInputStreamProvider(InputStream inputStream){
        _streamContents  = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    @Override
    public InputStream get() throws InputStreamProviderException {
        return new ByteArrayInputStream(_streamContents.getBytes());
    }
}
