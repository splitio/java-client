package io.split.engine.sse.dtos;

public class Notification {
    public enum Type {
        SPLIT_UPDATE,
        SPLIT_KILL,
        SEGMENT_UPDATE,
        CONTROL,
        OCCUPANCY,
        ERROR
    }
}
