package io.split.telemetry.storage;

import io.split.telemetry.domain.MethodExceptions;
import io.split.telemetry.domain.enums.MethodEnum;
import org.junit.Assert;
import org.junit.Test;

public class InMemoryTelemetryStorageTest{

    @Test
    public void testInMemoryTelemetryStorage() throws Exception {
        InMemoryTelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();

        telemetryStorage.recordException(MethodEnum.TREATMENT);
        telemetryStorage.recordException(MethodEnum.TREATMENTS);
        telemetryStorage.recordException(MethodEnum.TREATMENT);

        MethodExceptions methodExceptions = telemetryStorage.popExceptions();
        Assert.assertEquals(2, methodExceptions.get_treatment());
    }
}