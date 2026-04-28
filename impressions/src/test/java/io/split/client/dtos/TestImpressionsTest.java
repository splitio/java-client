package io.split.client.dtos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;

public class TestImpressionsTest {

    @Test
    public void TestShrinkedPropertyNames() {
        Gson gson = new Gson();
        TestImpressions imp = new TestImpressions("someTest", new ArrayList<>());
        String serialized = gson.toJson(imp);
        Map<String, Object> deSerialized = gson.fromJson(serialized, new TypeToken<Map<String, Object>>() { }.getType());
        Object featureName = deSerialized.get(TestImpressions.FIELD_TEST_NAME);
        assertThat(featureName, is(notNullValue()));
        assertThat(featureName, instanceOf(String.class));
        assertThat(featureName, is("someTest"));

        Object keyImpressions = deSerialized.get(TestImpressions.FIELD_KEY_IMPRESSIONS);
        assertThat(keyImpressions, is(notNullValue()));
        assertThat(keyImpressions, instanceOf(ArrayList.class));
        assertThat(keyImpressions, is(new ArrayList<KeyImpression>()));
    }
}
