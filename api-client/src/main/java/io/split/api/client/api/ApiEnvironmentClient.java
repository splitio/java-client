package io.split.api.client.api;

import io.split.api.client.EnvironmentClient;
import io.split.api.dtos.Environment;

import java.util.ArrayList;
import java.util.List;

public class ApiEnvironmentClient implements EnvironmentClient {
    public List<Environment> list() {
        return new ArrayList<>();
    }
}
