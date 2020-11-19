package io.split.engine.sse;

import io.split.engine.sse.client.SSEClient;
import org.apache.hc.core5.net.URIBuilder;
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


        SSEClient sse = new SSEClient(e -> { System.out.println(e); return null; },
                s -> { System.out.println(s); return null; });
        sse.open(uri);
        Thread.sleep(5000);
        sse.close();
        Thread.sleep(100000);
    }
}
