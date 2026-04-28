package io.split.client.dtos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;

public class KeyImpressionTest {

    @Test
    public void TestShrinkedPropertyNames() {
        Gson gson = new Gson();
        KeyImpression imp = new KeyImpression();
        imp.feature = "someFeature";
        imp.keyName = "someKey";
        imp.bucketingKey ="someBucketingKey";
        imp.treatment = "someTreatment";
        imp.label = "someLabel";
        imp.changeNumber = 123L;
        imp.time = 456L;
        imp.previousTime = 789L;
        imp.properties = "{\"name\": \"value\"}";

        String serialized = gson.toJson(imp);
        Map<String, Object> deSerialized = gson.fromJson(serialized, new TypeToken<Map<String, Object>>() { }.getType());

        // TODO: Assert no feature is added to the map.

        Object keyName = deSerialized.get(KeyImpression.FIELD_KEY_NAME);
        assertThat(keyName, is(notNullValue()));
        assertThat(keyName, instanceOf(String.class));
        assertThat(keyName, is("someKey"));

        Object bucketingKey = deSerialized.get(KeyImpression.FIELD_BUCKETING_KEY);
        assertThat(bucketingKey, is(notNullValue()));
        assertThat(bucketingKey, instanceOf(String.class));
        assertThat(bucketingKey, is("someBucketingKey"));

        Object treatment = deSerialized.get(KeyImpression.FIELD_TREATMENT);
        assertThat(treatment, is(notNullValue()));
        assertThat(treatment, instanceOf(String.class));
        assertThat(treatment, is("someTreatment"));

        Object label = deSerialized.get(KeyImpression.FIELD_LABEL);
        assertThat(label, is(notNullValue()));
        assertThat(label, instanceOf(String.class));
        assertThat(label, is("someLabel"));

        Object changeNumber = deSerialized.get(KeyImpression.FIELD_CHANGE_NUMBER);
        assertThat(changeNumber, is(notNullValue()));
        assertThat(changeNumber, instanceOf(Double.class));
        assertThat(changeNumber, is(123.0));

        Object time = deSerialized.get(KeyImpression.FIELD_TIME);
        assertThat(time, is(notNullValue()));
        assertThat(time, instanceOf(Double.class));
        assertThat(time, is(456.0));

        Object previousTime = deSerialized.get(KeyImpression.FIELD_PREVIOUS_TIME);
        assertThat(previousTime, is(notNullValue()));
        assertThat(previousTime, instanceOf(Double.class));
        assertThat(previousTime, is(789.0));

        Object properties = deSerialized.get(KeyImpression.FIELD_PROPERTIES);
        assertThat(properties, is(notNullValue()));
        assertThat(properties, instanceOf(String.class));
        assertThat(properties, is("{\"name\": \"value\"}"));

    }
}
