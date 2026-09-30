package io.split.client.lifecycle;

/**
 * Internal signals fed into the events manager (the {@code I} type parameter of the commons
 * {@code EventsManager<E, I, M>}). Not part of the supported public API.
 */
public enum SdkInternalEvent {
    SDK_READY,
    FLAGS_UPDATED,
    FLAG_KILLED_NOTIFICATION,
    SEGMENTS_UPDATED,
    RULE_BASED_SEGMENTS_UPDATED,
    SDK_READY_TIMEOUT_REACHED
}
