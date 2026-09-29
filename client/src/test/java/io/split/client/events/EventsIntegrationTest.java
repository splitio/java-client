package io.split.client.events;

import io.harness.events.EventsManager;
import io.harness.events.EventsManagerConfig;
import io.harness.events.EventsManagers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the jvm-commons events module is wired into the client build.
 */
public class EventsIntegrationTest {

    private enum External { SDK_READY }

    private enum Internal { SPLITS_LOADED, SEGMENTS_LOADED }

    @Test
    public void externalEventFiresOnceAllInternalEventsArrive() {
        EventsManagerConfig<External, Internal> config = EventsManagerConfig.<External, Internal>builder()
                .requireAll(External.SDK_READY, Internal.SPLITS_LOADED, Internal.SEGMENTS_LOADED)
                .executionLimit(External.SDK_READY, 1)
                .build();
        EventsManager<External, Internal, String> manager =
                EventsManagers.create(config, (handler, event, metadata) -> handler.handle(event, metadata));

        List<String> received = new ArrayList<>();
        manager.register(External.SDK_READY, (event, metadata) -> received.add(event + ":" + metadata));

        manager.notifyInternalEvent(Internal.SPLITS_LOADED, "splits");
        assertFalse(manager.eventAlreadyTriggered(External.SDK_READY));
        assertTrue(received.isEmpty());

        manager.notifyInternalEvent(Internal.SEGMENTS_LOADED, "segments");
        assertTrue(manager.eventAlreadyTriggered(External.SDK_READY));
        assertEquals(1, received.size());

        manager.destroy();
    }
}
