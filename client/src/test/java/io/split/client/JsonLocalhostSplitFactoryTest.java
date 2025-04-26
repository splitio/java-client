package io.split.client;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class JsonLocalhostSplitFactoryTest {

    @Test
    public void works() throws IOException, URISyntaxException, InterruptedException, TimeoutException {
        SplitClientConfig config = SplitClientConfig.builder()
                .splitFile("src/test/resources/splits_localhost.json")
                .segmentDirectory("src/test/resources")
                .setBlockUntilReadyTimeout(10000)
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
        client.destroy();
    }
}