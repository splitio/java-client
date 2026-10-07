package io.split.e2e;

import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

/**
 * Initialises the SDK the way a consumer of the published non-shaded artifact does (see build.gradle).
 * Localhost mode needs no network or keys, yet still builds the SDK events manager, so a class the
 * artifact does not bundle or declare surfaces here as a NoClassDefFoundError.
 */
public class NonShadedConsumerE2ETest {

    @Test
    public void initialisesAndEvaluatesFromTheNonShadedArtifact() throws Exception {
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
