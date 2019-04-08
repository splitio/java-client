package io.split.client;

import com.google.common.collect.Maps;
import io.split.grammar.Treatments;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for LocalhostSplitFactory
 *
 * @author adil
 */
public class LocalhostSplitFactoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void works() throws IOException {
        File file = folder.newFile(LocalhostSplitFactory.FILENAME);

        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("onboarding", "user2"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("test"), LocalhostSplit.of("a"));

        writeFile(file, map);

        LocalhostSplitFactory factory = new LocalhostSplitFactory(folder.getRoot().getAbsolutePath(), LocalhostSplitFactory.FILENAME);
        SplitClient client = factory.client();

        assertThat(client.getTreatment(null, "foo"), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("user1", "foo"), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("user1", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user2", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user3", "onboarding"), is(equalTo("on")));
        assertThat(client.getTreatment("user1", "test"), is(equalTo("a")));
        assertThat(client.getTreatment("user2", "test"), is(equalTo("a")));

        // now update it.
        map.clear();

        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));

        factory.updateFeatureToTreatmentMap(map);

        assertThat(client.getTreatment("user1", "onboarding"), is(equalTo("on")));
        assertThat(client.getTreatment("user2", "onboarding"), is(equalTo("on")));
        assertThat(client.getTreatment("user3", "onboarding"), is(equalTo("on")));
        assertThat(client.getTreatment("user1", "test"), is(equalTo(Treatments.CONTROL)));
    }

    private void writeFile(File f, Map<SplitAndKey, LocalhostSplit> map) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

        for (Map.Entry<SplitAndKey, LocalhostSplit> entry : map.entrySet()) {
            String line = toString(entry);
            writer.write(line);
        }

        writer.flush();
        writer.close();
    }

    private String toString(Map.Entry<SplitAndKey, LocalhostSplit> entry) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(entry.getKey().split());
        bldr.append(' ');
        bldr.append(entry.getValue().treatment);
        if (entry.getKey().key() != null) {
            bldr.append(' ');
            bldr.append(entry.getKey().key());
        }
        bldr.append('\n');
        return bldr.toString();
    }


}
