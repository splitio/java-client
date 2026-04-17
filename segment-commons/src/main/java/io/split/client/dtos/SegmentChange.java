package io.split.client.dtos;

import java.util.List;

public class SegmentChange {
    public String id;
    public String name;
    public List<String> added;
    public List<String> removed;
    public long since;
    public long till;
}
