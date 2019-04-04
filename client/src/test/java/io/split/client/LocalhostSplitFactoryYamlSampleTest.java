package io.split.client;

import com.google.common.collect.Maps;
import io.split.grammar.Treatments;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Tests the yaml template file located in the test resource folder
 *
 * @author patricioe
 */
public class LocalhostSplitFactoryYamlSampleTest {

    @Test
    public void works() throws IOException {

        String file = getClass().getClassLoader().getResource(LocalhostSplitFactory.FILENAME_YAML).getFile();

        LocalhostSplitFactory factory = new LocalhostSplitFactory("", file);
        SplitClient client = factory.client();

        assertThat(client.getTreatment(null, "foo"), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("user_a", "foo"), is(equalTo(Treatments.CONTROL)));

        assertThat(client.getTreatment("user_a", "split_1"), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_a", "split_1").treatment(), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_a", "split_1").config(), is(equalTo("{ \"size\" : 20 }")));

        assertThat(client.getTreatment("user_b", "split_1"), is(equalTo("on")));
        assertThat(client.getTreatmentWithConfig("user_b", "split_1").treatment(), is(equalTo("on")));
        assertThat(client.getTreatmentWithConfig("user_b", "split_1").config(), is(nullValue()));

        assertThat(client.getTreatment("user_a", "split_2"), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_a", "split_2").treatment(), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_a", "split_2").config(), is(equalTo("{ \"size\" : 20 }")));

        // Update

        Map<SplitAndKey, LocalhostSplit> update = Maps.newHashMap();
        update.put(SplitAndKey.of("split_2", "user_a"), LocalhostSplit.of("on"));

        factory.updateFeatureToTreatmentMap(update);

        assertThat(client.getTreatment("user_a", "split_2"), is(equalTo("on")));
    }

    private void writeFile(File f, StringWriter content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.write(content.toString());
        writer.flush();
        writer.close();
    }

    private String toString(Map.Entry<SplitAndKey, String> entry) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(entry.getKey().split());
        bldr.append(' ');
        bldr.append(entry.getValue());
        if (entry.getKey().key() != null) {
            bldr.append(' ');
            bldr.append(entry.getKey().key());
        }
        bldr.append('\n');
        return bldr.toString();
    }


}
