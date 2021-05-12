package io.split.telemetry.domain.enums;

public enum StreamEventsEnum {
    CONNECTION_ESTABLISHED(0),
    OCCUPANCY_PRI(10),
    OCCUPANCY_SEC(20),
    STREAMING_STATUS(30),
    SSE_CONNECTION_ERROR(40),
    TOKEN_REFRESH(50),
    ABLY_ERROR(60),
    SYNC_MODE_UPDATE(70);


    private int _type;

    StreamEventsEnum(int type) {
        _type = type;
    }

    public int get_type() {
        return _type;
    }
}
