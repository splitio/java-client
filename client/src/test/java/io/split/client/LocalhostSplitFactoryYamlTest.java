package io.split.client;

import io.split.Spec;
import io.split.client.utils.LocalhostUtils;
import io.split.grammar.Treatments;
import io.split.service.SplitHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;

/**
 * Tests for LocalhostSplitFactory for YAML files.
 *
 *
 * - split_1:
 *  {key: user_b, treatment: 'off', config: '{ "size" : 20 }'}
 * - split_1:
 *  {key: user_b, treatment: 'on'}
 * - split_2:
 *  {key: user_b, treatment: 'off', config: '{ "size" : 20 }'}
 *
 *
 * @author patricioe
 */
public class LocalhostSplitFactoryYamlTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void works() throws IOException, URISyntaxException {
        File file = folder.newFile(SplitClientConfig.LOCALHOST_DEFAULT_FILE);

        List<Map<String, Object>> allSplits = new ArrayList();

        Map<String, Object> split1_user_a = new LinkedHashMap<>();
        Map<String, Object> split1_user_a_data = new LinkedHashMap<>();
        split1_user_a_data.put("keys", "user_a");
        split1_user_a_data.put("treatment", "off");
        split1_user_a_data.put("config", "{ \"size\" : 20 }");
        split1_user_a.put("split_1", split1_user_a_data);
        allSplits.add(split1_user_a);

        Map<String, Object> split1_user_b = new LinkedHashMap<>();
        Map<String, Object> split1_user_b_data = new LinkedHashMap<>();
        split1_user_b_data.put("keys", "user_b");
        split1_user_b_data.put("treatment", "on");
        split1_user_b.put("split_1", split1_user_b_data);
        allSplits.add(split1_user_b);

        Map<String, Object> split2_user_a = new LinkedHashMap<>();
        Map<String, Object> split2_user_a_data = new LinkedHashMap<>();
        split2_user_a_data.put("keys", "user_a");
        split2_user_a_data.put("treatment", "off");
        split2_user_a_data.put("config", "{ \"size\" : 20 }");
        split2_user_a.put("split_2", split2_user_a_data);
        allSplits.add(split2_user_a);


        Yaml yaml = new Yaml();
        StringWriter writer = new StringWriter();
        yaml.dump(allSplits, writer);

        String expectedYaml = "- split_1: {keys: user_a, treatment: 'off', config: '{ \"size\" : 20 }'}\n" +
                "- split_1: {keys: user_b, treatment: 'on'}\n" +
                "- split_2: {keys: user_a, treatment: 'off', config: '{ \"size\" : 20 }'}\n";

        assertEquals(expectedYaml, writer.toString());

        LocalhostUtils.writeFile(file, writer);

        SplitClientConfig config = SplitClientConfig.builder().splitFile(file.getAbsolutePath()).build();
        Spec.SPEC_VERSION = Spec.SPEC_1_1; // check old spec
        SplitFactory splitFactory = SplitFactoryBuilder.build("localhost", config);
        SplitClient client = splitFactory.client();

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

        // unchanged
        assertThat(client.getTreatment("user_a", "split_1"), is(equalTo("off")));
        // unchanged
        assertThat(client.getTreatment("user_b", "split_1"), is(equalTo("on")));
    }
}