package io.split.client;

import io.split.Spec;
import io.split.grammar.Treatments;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

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
    public void works() throws IOException, URISyntaxException {

        String file = getClass().getClassLoader().getResource(SplitClientConfig.LOCALHOST_DEFAULT_FILE).getFile();

        SplitClientConfig config = SplitClientConfig.builder().splitFile(file).build();
        Spec.SPEC_VERSION = Spec.SPEC_1_1; // check old spec
        SplitFactory splitFactory = SplitFactoryBuilder.build("localhost", config);
        SplitClient client = splitFactory.client();

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

        assertThat(client.getTreatment("user_1", "splitWithKeys"), is(equalTo("v1")));
        assertThat(client.getTreatmentWithConfig("user_1", "splitWithKeys").treatment(), is(equalTo("v1")));
        assertThat(client.getTreatmentWithConfig("user_1", "splitWithKeys").config(), is(equalTo("{ \"size\" : 44 }")));

        assertThat(client.getTreatment("user_2", "splitWithKeys"), is(equalTo("v1")));
        assertThat(client.getTreatmentWithConfig("user_2", "splitWithKeys").treatment(), is(equalTo("v1")));
        assertThat(client.getTreatmentWithConfig("user_2", "splitWithKeys").config(), is(equalTo("{ \"size\" : 44 }")));

        assertThat(client.getTreatment("user_3", "splitWithKeys"), is(equalTo("v1")));
        assertThat(client.getTreatmentWithConfig("user_3", "splitWithKeys").treatment(), is(equalTo("v1")));
        assertThat(client.getTreatmentWithConfig("user_3", "splitWithKeys").config(), is(equalTo("{ \"size\" : 44 }")));

        assertThat(client.getTreatment("user_random", "splitWithNoKeys"), is(equalTo("v2")));
        assertThat(client.getTreatmentWithConfig("user_random", "splitWithNoKeys").treatment(), is(equalTo("v2")));
        assertThat(client.getTreatmentWithConfig("user_random", "splitWithNoKeys").config(), is(equalTo("{ \"size\" : 999 }")));
    }
}
