package io.split.api.client.local;

import io.split.api.client.EnvironmentClient;
import io.split.api.dtos.Environment;

import java.util.ArrayList;
import java.util.List;

public class LocalEnvironmentClient implements EnvironmentClient {
    @Override
    public List<Environment> list() {
        return new ArrayList<>();
    }

    @Override
    public Environment get(String name) {
        return null;
    }
}
