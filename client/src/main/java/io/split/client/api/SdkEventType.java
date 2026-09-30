package io.split.client.api;

/**
 * What kind of change triggered an {@link SdkEvent#SDK_UPDATE}.
 */
public enum SdkEventType {
    /** One or more feature flags were added, updated, removed or killed. */
    FLAGS_UPDATE,

    /** Segments or rule-based segments changed. */
    SEGMENTS_UPDATE
}
