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
 * Tests the yaml template file (compacted version using curly braces) located in the test resource folder
 *
 * @author patricioe
 */
public class LocalhostSplitFactoryYamlCompactSampleTest {

    @Test
    public void works() throws IOException, URISyntaxException {

        String file = getClass().getClassLoader().getResource("split_compact.yaml").getFile();

        SplitClientConfig config = SplitClientConfig.builder().splitFile(file).build();
        Spec.SPEC_VERSION = Spec.SPEC_1_1; // check old spec
        SplitFactory splitFactory = SplitFactoryBuilder.build("localhost", config);
        SplitClient client = splitFactory.client();

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
    }

    @Test
    public void worksYML() throws IOException, URISyntaxException {

        String file = getClass().getClassLoader().getResource("split_compact.yml").getFile();

        SplitClientConfig config = SplitClientConfig.builder().splitFile(file).build();
        Spec.SPEC_VERSION = Spec.SPEC_1_1; // check old spec
        SplitFactory splitFactory = SplitFactoryBuilder.build("localhost", config);
        SplitClient client = splitFactory.client();

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
    }
}