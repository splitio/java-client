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

    private final SdkEventType type;
    private final Set<String> names;
    private final boolean initialCacheLoad;
    private final Long lastUpdateTimestamp;

    private SdkEventMetadata(SdkEventType type, Set<String> names, boolean initialCacheLoad, Long lastUpdateTimestamp) {
        this.type = type;
        this.names = Collections.unmodifiableSet(names);
        this.initialCacheLoad = initialCacheLoad;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
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
        return type;
    }

    /** @return the affected flag names; never null, unmodifiable */
    public Set<String> getNames() {
        return names;
    }

    public boolean isInitialCacheLoad() {
        return initialCacheLoad;
    }

    /** @return milliseconds since epoch of the last update, or {@code null} if not applicable */
    public Long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
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
        return initialCacheLoad == that.initialCacheLoad
                && type == that.type
                && names.equals(that.names)
                && Objects.equals(lastUpdateTimestamp, that.lastUpdateTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, names, initialCacheLoad, lastUpdateTimestamp);
    }

    @Override
    public String toString() {
        return "SdkEventMetadata{type=" + type + ", names=" + names + ", initialCacheLoad=" + initialCacheLoad
                + ", lastUpdateTimestamp=" + lastUpdateTimestamp + "}";
    }
}
