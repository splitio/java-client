package io.split.client;

import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonResourceLocalhostSplitChangeFetcher extends JsonLocalhostSplitChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(JsonResourceLocalhostSplitChangeFetcher.class);
    private final String _fileName;

    public JsonResourceLocalhostSplitChangeFetcher(String fileName) {
        _fileName = fileName;
        super.lastHash = new byte[0];
    }

    @Override
    public JsonReader readFile() {
        try {
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(_fileName);
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            JsonReader jsonReader = new JsonReader(streamReader);
            return jsonReader;
        } catch (Exception e){
            _log.warn(String.format("Problem to fetch split change using the file %s",
                    _fileName), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFilePath() {
        return _fileName;
    }
}