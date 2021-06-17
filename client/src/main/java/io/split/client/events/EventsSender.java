package io.split.client.events;

import io.split.client.dtos.Event;
import io.split.service.HttpPostImp;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.net.URI;
import java.util.List;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class EventsSender {
    private final URI _endpoint;
    private final CloseableHttpClient _client;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final HttpPostImp _httpPostImp;

    public static EventsSender create(CloseableHttpClient httpclient, URI eventsTarget, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        return new EventsSender(httpclient, eventsTarget, telemetryRuntimeProducer);
    }

    EventsSender(CloseableHttpClient httpclient, URI eventsTarget, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = checkNotNull(httpclient);
        _endpoint = checkNotNull(eventsTarget);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _httpPostImp = new HttpPostImp(httpclient, telemetryRuntimeProducer);
    }

    public void sendEvents(List<Event> _data) {
        _httpPostImp.post(_endpoint, _data, "Events ", HTTPLatenciesEnum.EVENTS, LastSynchronizationRecordsEnum.EVENTS, ResourceEnum.EVENT_SYNC);
    }
}
