package io.split.client.lifecycle;

import io.split.client.api.SdkEventMetadata;

/**
 * Write-path hook used by the sync engine to report that cached data changed. Implementations must not throw.
 */
public interface SdkEventsNotifier {

    SdkEventsNotifier NOOP = (event, metadata) -> { };

    void notify(SdkInternalEvent event, SdkEventMetadata metadata);
}
