package io.split.engine.sse.dtos;

public class ControlNotification extends IncomingNotification {
    private ControlType controlType;

    public ControlType getControlType() {
        return controlType;
    }

    public void setControlType(ControlType controlType) {
        this.controlType = controlType;
    }
}
