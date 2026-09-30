package io.split.client.api;

/**
 * SDK lifecycle events a consumer can subscribe to.
 *
 * <p>{@code sdkReadyFromCache} is intentionally not part of this enum: it only applies to SDKs with a persistent cache.
 */
public enum SdkEvent {
    /**
     * The SDK has fetched feature flags and segments and is ready to evaluate. Fires once and is replayed
     * (with {@code null} metadata) to listeners registered after it happened.
     * Metadata: {@link SdkEventMetadata#isInitialCacheLoad()} and {@link SdkEventMetadata#getLastUpdateTimestamp()}.
     */
    SDK_READY,

    /**
     * The SDK did not become ready within the configured {@code blockUntilReady} time. Metadata is always {@code null}.
     */
    SDK_READY_TIMED_OUT,

    /**
     * Feature flags or segments changed after the SDK became ready. Fires every time, never replayed.
     * Metadata: {@link SdkEventMetadata#getType()} and {@link SdkEventMetadata#getNames()}.
     */
    SDK_UPDATE
}
