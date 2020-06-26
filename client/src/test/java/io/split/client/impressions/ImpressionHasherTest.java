package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


public class ImpressionHasherTest {

    @Test
    public void works() {
        KeyImpression imp1 = new KeyImpression();
        imp1.feature = "someFeature";
        imp1.keyName = "someKey";
        imp1.changeNumber = 123L;
        imp1.label = "someLabel";

        // Different feature
        KeyImpression imp2 = new KeyImpression();
        imp2.feature = "someOtherFeature";
        imp2.keyName = "someKey";
        imp2.changeNumber = 123L;
        imp2.label = "someLabel";
        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));

        // different key
        imp2.feature = imp1.feature;
        imp2.keyName = "someOtherKey";
        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));

        // different changeNumber
        imp2.keyName = imp1.keyName;
        imp2.changeNumber = 456L;
        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));

        // different label
        imp2.changeNumber = imp1.changeNumber;
        imp2.label = "someOtherLabel";
        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));
    }

    @Test
    public void doesNotCrash() {
        KeyImpression imp1 = new KeyImpression();
        imp1.feature = null;
        imp1.keyName = "someKey";
        imp1.changeNumber = 123L;
        imp1.label = "someLabel";
        assertNotNull(ImpressionHasher.process(imp1));

        imp1.keyName = null;
        assertNotNull(ImpressionHasher.process(imp1));

        imp1.changeNumber = null;
        assertNotNull(ImpressionHasher.process(imp1));

        imp1.label = null;
        assertNotNull(ImpressionHasher.process(imp1));

        assertNull(ImpressionHasher.process(null));
    }
}
