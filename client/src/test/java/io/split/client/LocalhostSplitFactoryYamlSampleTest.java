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
 * Tests the yaml template file located in the test resource folder
 *
 * @author patricioe
 */
public class LocalhostSplitFactoryYamlSampleTest {

    @Test
    public void works() throws IOException {

        String file = getClass().getClassLoader().getResource(SplitClientConfig.LOCALHOST_DEFAULT_FILE).getFile();

        LocalhostSplitFactory factory = new LocalhostSplitFactory("", file);
        SplitClient client = factory.client();

        assertThat(client.getTreatment(null, "foo"), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("user_a", "foo"), is(equalTo(Treatments.CONTROL)));

        assertThat(client.getTreatment("user_a", "split_1"), is(equalTo("on")));
        assertThat(client.getTreatmentWithConfig("user_a", "split_1").treatment(), is(equalTo("on")));
        assertThat(client.getTreatmentWithConfig("user_a", "split_1").config(), is(nullValue()));

        assertThat(client.getTreatment("user_b", "split_1"), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_b", "split_1").treatment(), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_b", "split_1").config(), is(equalTo("{ \"size\" : 20 }")));

        assertThat(client.getTreatment("user_b", "split_2"), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_b", "split_2").treatment(), is(equalTo("off")));
        assertThat(client.getTreatmentWithConfig("user_b", "split_2").config(), is(equalTo("{ \"size\" : 44 }")));

        // Update

        Map<SplitAndKey, LocalhostSplit> update = Maps.newHashMap();
        update.put(SplitAndKey.of("split_2", "user_a"), LocalhostSplit.of("on"));

        factory.updateFeatureToTreatmentMap(update);

        assertThat(client.getTreatment("user_a", "split_2"), is(equalTo("on")));
    }

}
