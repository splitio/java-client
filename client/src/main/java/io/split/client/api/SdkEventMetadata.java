package io.split.client.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable details of a {@link SdkEvent}.
 *
 * <p>The metadata handed to a {@link SdkEventListener} can itself be {@code null}: always for
 * {@link SdkEvent#SDK_READY_TIMED_OUT}, and for a once-only event replayed to a late subscriber.
 */
public final class SdkEventMetadata {

    private final SdkEventType _type;
    private final Set<String> _names;
    private final boolean _initialCacheLoad;
    private final Long _lastUpdateTimestamp;

    private SdkEventMetadata(SdkEventType type, Set<String> names, boolean initialCacheLoad, Long lastUpdateTimestamp) {
        _type = type;
        _names = Collections.unmodifiableSet(names);
        _initialCacheLoad = initialCacheLoad;
        _lastUpdateTimestamp = lastUpdateTimestamp;
    }

    /**
     * Metadata for {@link SdkEvent#SDK_UPDATE}. Null and blank names are dropped.
     */
    public static SdkEventMetadata update(SdkEventType type, Collection<String> names) {
        Set<String> sanitized = new HashSet<>();
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.trim().isEmpty()) {
                    sanitized.add(name);
                }
            }
        }
        return new SdkEventMetadata(type, sanitized, false, null);
    }

    /**
     * Metadata for {@link SdkEvent#SDK_READY}.
     */
    public static SdkEventMetadata ready(boolean initialCacheLoad, Long lastUpdateTimestamp) {
        return new SdkEventMetadata(null, new HashSet<String>(), initialCacheLoad, lastUpdateTimestamp);
    }

    /** @return the update type, or {@code null} for SDK_READY metadata */
    public SdkEventType getType() {
        return _type;
    }

    /** @return the affected flag names; never null, unmodifiable */
    public Set<String> getNames() {
        return _names;
    }

    public boolean isInitialCacheLoad() {
        return _initialCacheLoad;
    }

    /** @return milliseconds since epoch of the last update, or {@code null} if not applicable */
    public Long getLastUpdateTimestamp() {
        return _lastUpdateTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdkEventMetadata that = (SdkEventMetadata) o;
        return _initialCacheLoad == that._initialCacheLoad
                && _type == that._type
                && _names.equals(that._names)
                && Objects.equals(_lastUpdateTimestamp, that._lastUpdateTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_type, _names, _initialCacheLoad, _lastUpdateTimestamp);
    }

    @Override
    public String toString() {
        return "SdkEventMetadata{type=" + _type + ", names=" + _names + ", initialCacheLoad=" + _initialCacheLoad
                + ", lastUpdateTimestamp=" + _lastUpdateTimestamp + "}";
    }
}
