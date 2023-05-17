package io.split.engine.sse.enums;

public enum CompressType {
    NOT_COMPRESSED(0),
    GZIP(1),
    ZLIB(2);

    private long _value;

    CompressType(long value) {
        _value = value;
    }

    public long getValue() {
        return _value;
    }
}