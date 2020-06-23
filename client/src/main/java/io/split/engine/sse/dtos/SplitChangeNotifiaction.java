package io.split.engine.sse.dtos;

public class SplitChangeNotifiaction extends IncomingNotification {
    private String changeNumber;

    public String getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(String changeNumber) {
        this.changeNumber = changeNumber;
    }
}
