package io.split.client.impressions;

public interface Filter {

    boolean add(String data);
    boolean contains(String data);
    void clear();
}
