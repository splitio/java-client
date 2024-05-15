package io.split.client.events;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.Event;
import io.split.client.utils.Utils;
import io.split.service.HttpPostImp;
import io.split.service.SplitHttpClient;
import io.split.telemetry.domain.enums.HttpParamsWrapper;
import io.split.telemetry.storage.TelemetryRuntimeProducer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class EventsSender {

    private static final String BULK_ENDPOINT_PATH = "api/events/bulk";
    private final URI _bulkEndpoint;
    private final SplitHttpClient _client;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final HttpPostImp _httpPostImp;

    public static EventsSender create(SplitHttpClient splitHttpclient, URI eventsTarget, TelemetryRuntimeProducer telemetryRuntimeProducer)
            throws URISyntaxException {
        return new EventsSender(splitHttpclient, Utils.appendPath(eventsTarget, BULK_ENDPOINT_PATH), telemetryRuntimeProducer);
    }

    EventsSender(SplitHttpClient splitHttpclient, URI eventsTarget, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = splitHttpclient;
        _bulkEndpoint = checkNotNull(eventsTarget);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _httpPostImp = new HttpPostImp(_client, telemetryRuntimeProducer);
    }

    public void sendEvents(List<Event> _data) {
        _httpPostImp.post(_bulkEndpoint, _data, "Events ", HttpParamsWrapper.EVENTS);
    }

    @VisibleForTesting
    URI getBulkEndpoint() {
        return _bulkEndpoint;
    }
}
