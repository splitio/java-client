package io.split.e2e;

import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

/**
 * Initialises the SDK from the default (fully shaded) artifact with nothing else on the classpath: guava, httpclient5
 * and snakeyaml must all come from inside the jar, which is what the fat jar promises its consumers.
 */
public class FatJarConsumerE2ETest {

    @Test
    public void initialisesAndEvaluatesFromTheFatJarAlone() throws Exception {
        URL splits = getClass().getClassLoader().getResource("splits.yaml");
        Assert.assertNotNull("splits.yaml test resource missing", splits);

        SplitClientConfig config = SplitClientConfig.builder()
                .splitFile(splits.getPath())
                .setBlockUntilReadyTimeout(10000)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("localhost", config);
        try {
            SplitClient client = factory.client();
            client.blockUntilReady();
            Assert.assertEquals("on", client.getTreatment("any_user", "e2e_feature"));
        } finally {
            factory.destroy();
        }
    }
}
