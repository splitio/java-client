package io.split.client.lifecycle;

import io.harness.events.EventsManagerConfig;
import io.split.client.api.SdkEvent;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertTrue;

public class SdkEventsConfigTest {

    @Test
    public void executionLimitsMatchOnceOnlyAndUnlimitedEvents() {
        EventsManagerConfig<SdkEvent, SdkInternalEvent> config = SdkEventsConfig.build();
        assertThat(config.getExecutionLimits().get(SdkEvent.SDK_READY), is(1));
        assertThat(config.getExecutionLimits().get(SdkEvent.SDK_READY_TIMED_OUT), is(1));
        assertThat(config.getExecutionLimits().get(SdkEvent.SDK_UPDATE), is(-1));
    }

    @Test
    public void sdkUpdateRequiresSdkReadyAsPrerequisite() {
        EventsManagerConfig<SdkEvent, SdkInternalEvent> config = SdkEventsConfig.build();
        assertThat(config.getPrerequisites().get(SdkEvent.SDK_UPDATE), is(Collections.singleton(SdkEvent.SDK_READY)));
    }

    @Test
    public void evaluationOrderPlacesSdkReadyBeforeSdkUpdate() {
        EventsManagerConfig<SdkEvent, SdkInternalEvent> config = SdkEventsConfig.build();
        java.util.List<SdkEvent> order = config.getEvaluationOrder();
        assertTrue(order.indexOf(SdkEvent.SDK_READY) < order.indexOf(SdkEvent.SDK_UPDATE));
    }

    @Test
    public void sdkUpdateRequiresAnyOfTheFourUpdateInternalEvents() {
        EventsManagerConfig<SdkEvent, SdkInternalEvent> config = SdkEventsConfig.build();
        java.util.Set<java.util.Set<SdkInternalEvent>> groups = config.getRequireAny().get(SdkEvent.SDK_UPDATE);
        assertThat(groups, hasItem(Collections.singleton(SdkInternalEvent.FLAGS_UPDATED)));
        assertThat(groups, hasItem(Collections.singleton(SdkInternalEvent.FLAG_KILLED_NOTIFICATION)));
        assertThat(groups, hasItem(Collections.singleton(SdkInternalEvent.SEGMENTS_UPDATED)));
        assertThat(groups, hasItem(Collections.singleton(SdkInternalEvent.RULE_BASED_SEGMENTS_UPDATED)));
    }

    @Test
    public void buildDoesNotThrow() {
        // Cycle detection runs inside build(); a circular prerequisite/suppressedBy graph would throw here.
        SdkEventsConfig.build();
    }
}
