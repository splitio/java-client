package io.split.client.api;

/**
 * Callback invoked when a {@link SdkEvent} fires.
 */
@FunctionalInterface
public interface SdkEventListener {
    /**
     * @param metadata event details; may be {@code null} for {@link SdkEvent#SDK_READY_TIMED_OUT}
     *                 and for a {@link SdkEvent#SDK_READY} replayed to a late subscriber
     */
    void handle(SdkEventMetadata metadata);
}
