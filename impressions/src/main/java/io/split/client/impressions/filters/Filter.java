package io.split.client.impressions.filters;

public interface Filter {

    boolean add(String data);
    boolean contains(String data);
    void clear();
}
