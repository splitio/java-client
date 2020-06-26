package io.split.engine.sse.dtos;

public abstract class IncomingNotification {
    public enum Type {
        SPLIT_UPDATE,
        SPLIT_KILL,
        SEGMENT_UPDATE,
        CONTROL,
        OCCUPANCY
    }

    private final Type type;
    private final String channel;

    public IncomingNotification(Type type, String channel) {
        this.type = type;
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }

    public Type getType() {
        return type;
    }
}
