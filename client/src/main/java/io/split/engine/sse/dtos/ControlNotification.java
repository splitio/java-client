package io.split.engine.sse.dtos;

public class ControlNotification extends IncomingNotification {
    private ControlType controlType;

    public ControlNotification(String channel, ControlType controlType) {
        super(Type.CONTROL, channel);
        this.controlType = controlType;
    }

    public ControlType getControlType() {
        return controlType;
    }
}
