package io.split.client.lifecycle;

import io.harness.events.EventsManagerConfig;
import io.split.client.api.SdkEvent;

import java.util.Collections;

/**
 * Declarative rule graph wiring {@link SdkInternalEvent}s to the public {@link SdkEvent}s.
 *
 * <p>Package-private: this is the only place a raw commons {@code EventsManagerConfig} type is used, keeping it
 * unreachable from outside {@code io.split.client.lifecycle}.
 */
final class SdkEventsConfig {

    private SdkEventsConfig() {
        // utility class
    }

    static EventsManagerConfig<SdkEvent, SdkInternalEvent> build() {
        return EventsManagerConfig.<SdkEvent, SdkInternalEvent>builder()
                .requireAll(SdkEvent.SDK_READY, SdkInternalEvent.SDK_READY)
                .executionLimit(SdkEvent.SDK_READY, 1)
                .metadataSource(SdkEvent.SDK_READY, SdkInternalEvent.SDK_READY)

                .requireAll(SdkEvent.SDK_READY_TIMED_OUT, SdkInternalEvent.SDK_READY_TIMEOUT_REACHED)
                .executionLimit(SdkEvent.SDK_READY_TIMED_OUT, 1)
                .metadataSource(SdkEvent.SDK_READY_TIMED_OUT, SdkInternalEvent.SDK_READY_TIMEOUT_REACHED)

                .requireAny(SdkEvent.SDK_UPDATE,
                        SdkInternalEvent.FLAGS_UPDATED,
                        SdkInternalEvent.FLAG_KILLED_NOTIFICATION,
                        SdkInternalEvent.SEGMENTS_UPDATED,
                        SdkInternalEvent.RULE_BASED_SEGMENTS_UPDATED)
                .prerequisite(SdkEvent.SDK_UPDATE, SdkEvent.SDK_READY)
                .executionLimit(SdkEvent.SDK_UPDATE, -1)
                .metadataSource(SdkEvent.SDK_UPDATE,
                        Collections.singleton(SdkInternalEvent.FLAGS_UPDATED), SdkInternalEvent.FLAGS_UPDATED)
                .metadataSource(SdkEvent.SDK_UPDATE,
                        Collections.singleton(SdkInternalEvent.FLAG_KILLED_NOTIFICATION), SdkInternalEvent.FLAG_KILLED_NOTIFICATION)
                .metadataSource(SdkEvent.SDK_UPDATE,
                        Collections.singleton(SdkInternalEvent.SEGMENTS_UPDATED), SdkInternalEvent.SEGMENTS_UPDATED)
                .metadataSource(SdkEvent.SDK_UPDATE,
                        Collections.singleton(SdkInternalEvent.RULE_BASED_SEGMENTS_UPDATED), SdkInternalEvent.RULE_BASED_SEGMENTS_UPDATED)
                .build();
    }
}
