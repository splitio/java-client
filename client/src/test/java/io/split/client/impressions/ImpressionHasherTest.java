package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


public class ImpressionHasherTest {

    @Test
    public void works() {
        Impression imp1 = new Impression("someKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null, null);

        // Different feature
        Impression imp2 = new Impression("someKey",
                null,
                "someOtherFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null, null);

        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));

        // different key
        imp2 = new Impression("someOtherKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null, null);
        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));

        // different changeNumber
        imp2 = new Impression("someKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                456L,
                null, null);
        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));

        // different label
        imp2 = new Impression("someKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someOtherLabel",
                123L,
                null, null);
        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));

        // different treatment
        imp2 = new Impression("someKey",
                null,
                "someFeature",
                "someOtherTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null, null);

        assertThat(ImpressionHasher.process(imp1), not(equalTo(ImpressionHasher.process(imp2))));
    }

    @Test
    public void doesNotCrash() {
        Impression imp1 = new Impression("someKey",
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null, null);
        assertNotNull(ImpressionHasher.process(imp1));

        imp1 = new Impression(null,
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null, null);
        assertNotNull(ImpressionHasher.process(imp1));

        imp1 = new Impression(null,
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                null,
                null, null);
        assertNotNull(ImpressionHasher.process(imp1));

        imp1 = new Impression(null,
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                null,
                null,
                null, null);
        assertNotNull(ImpressionHasher.process(imp1));

        imp1 = new Impression(null,
                null,
                null,
                null,
                System.currentTimeMillis(),
                "someLabel",
                null,
                null, null);
        assertNotNull(ImpressionHasher.process(imp1));
        assertNull(ImpressionHasher.process(null));
    }
}
