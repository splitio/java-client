package io.split.engine.sse;

import io.split.engine.sse.client.SSEClient;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;

public class SSEClientTest {

    @Test
    @Ignore
    public void basicUsageTest() throws URISyntaxException, InterruptedException {
        URI uri = new URIBuilder("https://streaming.split.io/sse")
                .addParameter("accessToken", "X")
                .addParameter("v", "1,1")
                .addParameter("channels", "[?occupancy=metrics.publishers]control_pri")
                .build();
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(70000))
                .build();

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);

        CloseableHttpClient httpClient =  httpClientbuilder.build();

        SSEClient sse = new SSEClient(e -> null,
                s -> null, httpClient, telemetryRuntimeProducer, null);
        sse.open(uri);
        Thread.sleep(5000);
        sse.close();
        Thread.sleep(100000);
    }
}
