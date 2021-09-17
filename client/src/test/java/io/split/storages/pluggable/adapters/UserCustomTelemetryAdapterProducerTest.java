package io.split.storages.pluggable.adapters;

import io.split.client.utils.SDKMetadata;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class UserCustomTelemetryAdapterProducerTest {

    private CustomStorageWrapper _customStorageWrapper;
    private SafeUserStorageWrapper _safeUserStorageWrapper;
    private UserCustomTelemetryAdapterProducer _userCustomTelemetryAdapterProducer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _safeUserStorageWrapper = Mockito.mock(SafeUserStorageWrapper.class);
        _userCustomTelemetryAdapterProducer = new UserCustomTelemetryAdapterProducer(_customStorageWrapper, Mockito.mock(SDKMetadata.class));
        Field userCustomTelemetryAdapterProducer = UserCustomTelemetryAdapterProducer.class.getDeclaredField("_safeUserStorageWrapper");
        userCustomTelemetryAdapterProducer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomTelemetryAdapterProducer, userCustomTelemetryAdapterProducer.getModifiers() & ~Modifier.FINAL);
        userCustomTelemetryAdapterProducer.set(_userCustomTelemetryAdapterProducer, _safeUserStorageWrapper);

    }

    @Test
    public void testRecordLatency() {
        _userCustomTelemetryAdapterProducer.recordLatency(MethodEnum.TRACK, 10l);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).increment(Mockito.anyString(), Mockito.anyLong());
    }

    @Test
    public void testRecordException() {
        _userCustomTelemetryAdapterProducer.recordException(MethodEnum.TRACK);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).increment(Mockito.anyString(), Mockito.anyLong());
    }
}