package io.split.engine.sse.dtos;

public class SplitChangeNotifiaction extends IncomingNotification {
    private long changeNumber;

    public long getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(long changeNumber) {
        this.changeNumber = changeNumber;
    }
}
