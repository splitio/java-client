package io.split.engine.sse.enums;

import java.util.HashMap;
import java.util.Map;

public enum CompressType {
    NOT_COMPRESSED(0),
    GZIP(1),
    ZLIB(2);

    private final Integer value;

    CompressType(Integer value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    // Mapping compress type to compress type id
    private static final Map<Integer, CompressType> _map = new HashMap<>();
    static {
        for (CompressType compressType : CompressType.values())
            _map.put(compressType.value, compressType);
    }

    /**
     * Get compress type from value
     * @param value value
     * @return CompressType
     */
    public static CompressType from(Integer value) {
        if (value == null || _map.size() <= value){
            return null;
        }
        return _map.get(value);
    }
}