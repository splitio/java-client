package io.split.client.dtos;

public class ExcludedSegments {
    public ExcludedSegments() {}
    public ExcludedSegments(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String type;
    public String name;
}
