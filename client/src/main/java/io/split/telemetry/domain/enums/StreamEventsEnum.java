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

    public int getType() {
        return _type;
    }

    public enum StreamingStatusValues {
        STREAMING_DISABLED(0),
        STREAMING_PAUSED(2),
        STREAMING_ENABLED(1);

        private long _value;

        StreamingStatusValues(long value) {
            _value = value;
        }

        public long getValue() {
            return _value;
        }
    }

    public enum SseConnectionErrorValues {
        REQUESTED_CONNECTION_ERROR(0),
        NON_REQUESTED_CONNECTION_ERROR (1);

        private long _value;

        SseConnectionErrorValues(long value) {
            _value = value;
        }

        public long getValue() {
            return _value;
        }
    }

    public enum SyncModeUpdateValues {
        STREAMING_EVENT(0),
        POLLING_EVENT(1);

        private long _value;

        SyncModeUpdateValues(long value) {
            _value = value;
        }

        public long getValue() {
            return _value;
        }
    }
}
