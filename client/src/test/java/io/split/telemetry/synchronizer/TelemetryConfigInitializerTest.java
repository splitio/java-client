package io.split.telemetry.synchronizer;

import io.split.client.SplitClientConfig;
import io.split.engine.SDKReadinessGates;
import org.junit.Test;
import org.mockito.Mockito;

public class TelemetryConfigInitializerTest {

    @Test
    public void testRun() {
        SynchronizerMemory synchronizerMemory = Mockito.mock(SynchronizerMemory.class);
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);
        Mockito.when(gates.isSDKReadyNow()).thenReturn(true);
        TelemetryConfigInitializer telemetryConfigInitializer = new TelemetryConfigInitializer(synchronizerMemory, gates, config);
        Mockito.verify(synchronizerMemory, Mockito.times(1)).synchronizeConfig(Mockito.anyObject(),Mockito.anyLong(), Mockito.anyObject(), Mockito.anyObject());
    }

}