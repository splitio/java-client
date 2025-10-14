package io.split.client;

import io.split.client.dtos.FallbackTreatment;
import io.split.client.dtos.FallbackTreatmentsConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class JsonLocalhostSplitFactoryTest {

    @Test
    public void works() throws IOException, URISyntaxException, InterruptedException, TimeoutException {
        FallbackTreatmentsConfiguration fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(new FallbackTreatment("on-global"),
                new HashMap<String, FallbackTreatment>() {{ put("feature", new FallbackTreatment("off-local", "{\"prop2\", \"val2\"}")); }});

        SplitClientConfig config = SplitClientConfig.builder()
                .splitFile("src/test/resources/splits_localhost.json")
                .segmentDirectory("src/test/resources")
                .setBlockUntilReadyTimeout(10000)
                .fallbackTreatments(fallbackTreatmentsConfiguration)
                .build();
        SplitFactory splitFactory = SplitFactoryBuilder.build("localhost", config);
        SplitClient client = splitFactory.client();
        client.blockUntilReady();

        Assert.assertEquals("on", client.getTreatment("bilal@@split.io", "rbs_flag", new HashMap<String, Object>() {{
            put("email", "bilal@@split.io");
        }}));
        Assert.assertEquals("off", client.getTreatment("mauro@split.io", "rbs_flag", new HashMap<String, Object>() {{
            put("email", "mauro@split.io");
        }}));
        Assert.assertEquals("off", client.getTreatment("bilal", "test_split"));
        Assert.assertEquals("on", client.getTreatment("bilal", "push_test"));
        Assert.assertEquals("on_whitelist", client.getTreatment("admin", "push_test"));
        Assert.assertEquals("off-local", client.getTreatment("bilal", "feature"));
        Assert.assertEquals("on-global", client.getTreatment("bilal", "feature2"));

        client.destroy();
    }

    @Test
    public void testOldSpec() throws IOException, URISyntaxException, InterruptedException, TimeoutException {
        SplitClientConfig config = SplitClientConfig.builder()
                .splitFile("src/test/resources/split_old_spec.json")
                .segmentDirectory("src/test/resources")
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactory splitFactory = SplitFactoryBuilder.build("localhost", config);
        SplitClient client = splitFactory.client();
        client.blockUntilReady();

        Assert.assertEquals("on", client.getTreatment("bilal", "split_1"));
        Assert.assertEquals("off", client.getTreatment("bilal", "split_2"));
        Assert.assertEquals("v5", client.getTreatment("admin", "split_2"));
        client.destroy();
    }
}