package io.split.api.client;

import io.split.api.dtos.Environment;

import java.util.List;

public interface EnvironmentClient {
    List<Environment> list();

    Environment get(String name);
}
