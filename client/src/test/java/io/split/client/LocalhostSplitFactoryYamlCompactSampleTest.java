package io.split.client;

import com.google.common.collect.Maps;
import io.split.grammar.Treatments;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Tests the yaml template file (compacted version using curly braces) located in the test resource folder
 *
 * @author patricioe
 */
public class LocalhostSplitFactoryYamlCompactSampleTest {

    @Test
    public void works() throws IOException {

        String file = getClass().getClassLoader().getResource(".split_compact.yaml").getFile();

        LocalhostSplitFactory factory = new LocalhostSplitFactory("", file);
        SplitClient client = factory.client();

        assertThat(client.getTreatment(null, "foo"), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("user_c", "foo"), is(equalTo(Treatments.CONTROL)));

        assertThat(client.getTreatment("user_c", "split_1"), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_c", "split_1").treatment(), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_c", "split_1").config(), is(equalTo("{ \"size\" : 10 }")));

        assertThat(client.getTreatment("user_d", "split_1"), is(equalTo("on")));
        assertThat(client.getTreatmentWithConfig("user_d", "split_1").treatment(), is(equalTo("on")));
        assertThat(client.getTreatmentWithConfig("user_d", "split_1").config(), is(nullValue()));

        assertThat(client.getTreatment("user_e", "split_2"), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_e", "split_2").treatment(), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_e", "split_2").config(), is(equalTo("{ \"size\" : 55 }")));

        // Update

        Map<SplitAndKey, LocalhostSplit> update = Maps.newHashMap();
        update.put(SplitAndKey.of("split_2", "user_a"), LocalhostSplit.of("on"));

        factory.updateFeatureToTreatmentMap(update);

        assertThat(client.getTreatment("user_a", "split_2"), is(equalTo("on")));
    }

}
