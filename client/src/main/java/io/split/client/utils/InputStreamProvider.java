package io.split.client.utils;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface InputStreamProvider {

    public InputStream get() throws FileNotFoundException;
}
