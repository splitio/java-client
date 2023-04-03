package io.split.client;

import com.google.common.collect.Maps;
import io.split.client.utils.LocalhostUtils;
import io.split.grammar.Treatments;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests for LocalhostSplitFactory
 *
 * @author adil
 */
public class LocalhostSplitFactoryTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void works() throws IOException, URISyntaxException, InterruptedException {
        File file = folder.newFile(LocalhostSplitFactory.FILENAME);

        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("onboarding", "user2"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("test"), LocalhostSplit.of("a"));

        LocalhostUtils.writeFile(file, map);

        SplitClientConfig config = SplitClientConfig.builder().splitFile(folder.getRoot().getAbsolutePath()).build();
        SplitFactory splitFactory = SplitFactoryBuilder.build("localhost", config);
        SplitClient client = splitFactory.client();

        assertEquals(Treatments.CONTROL, client.getTreatment(null, "foo"));
        assertEquals(Treatments.CONTROL, client.getTreatment("user1", "foo"));
        assertEquals("off", client.getTreatment("user1", "onboarding"));
        assertEquals("off", client.getTreatment("user1", "onboarding"));
        assertEquals("off", client.getTreatment("user2", "onboarding"));
        assertEquals("on", client.getTreatment("user3", "onboarding"));
        assertEquals("a", client.getTreatment("user1", "test"));
        assertEquals("a", client.getTreatment("user2", "test"));
    }
}