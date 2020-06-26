package io.split.engine.sse.dtos;

public class SplitChangeNotification extends IncomingNotification {
    private final long changeNumber;

    public SplitChangeNotification(GenericNotificationData genericNotificationData) {
        super(Type.SPLIT_UPDATE, genericNotificationData.getChannel());
        this.changeNumber = genericNotificationData.getChangeNumber();
    }

    public long getChangeNumber() {
        return changeNumber;
    }
}
