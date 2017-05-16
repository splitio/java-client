package io.split.api.client.api;

import io.split.api.client.EnvironmentClient;
import io.split.api.dtos.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApiEnvironmentClient implements EnvironmentClient {
    public List<Environment> list() {
        return new ArrayList<>();
    }

    @Override
    public Environment get(String name) {
        for (Environment environment : list()) {
            if (Objects.equals(name, environment.name())) {
                return environment;
            }
        }
        return null;
    }
}
