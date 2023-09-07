package io.split.client.utils;

import io.split.client.exceptions.InputStreamProviderException;
import org.junit.Test;

public class FileInputStreamProviderTest {

    @Test(expected = InputStreamProviderException.class)
    public void processTestForException() throws InputStreamProviderException {
        FileInputStreamProvider fileInputStreamProvider = new FileInputStreamProvider("src/test/resources/notExist.json");
        fileInputStreamProvider.get();
    }
}