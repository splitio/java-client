package io.split.engine.sse;

import io.split.engine.sse.client.SSEClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.Ignore;
import org.junit.Test;

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

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(70000))
                .build();

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);

        CloseableHttpClient httpClient =  httpClientbuilder.build();

        SSEClient sse = new SSEClient(e -> { System.out.println(e); return null; },
                s -> { System.out.println(s); return null; }, httpClient);
        sse.open(uri);
        Thread.sleep(5000);
        sse.close(true);
        Thread.sleep(100000);
    }
}
