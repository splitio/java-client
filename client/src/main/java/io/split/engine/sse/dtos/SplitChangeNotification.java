package io.split.engine.sse.dtos;

public class SplitChangeNotification extends IncomingNotification {
    private final long changeNumber;

    public SplitChangeNotification(String channel, long changeNumber) {
        super(Type.SPLIT_UPDATE, channel);
        this.changeNumber = changeNumber;
    }

    public long getChangeNumber() {
        return changeNumber;
    }
}
