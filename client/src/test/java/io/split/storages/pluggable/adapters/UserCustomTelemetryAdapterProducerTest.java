package io.split.storages.pluggable.adapters;

import io.split.client.utils.SDKMetadata;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.telemetry.domain.enums.MethodEnum;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class UserCustomTelemetryAdapterProducerTest {

    private CustomStorageWrapper _customStorageWrapper;
    private UserStorageWrapper _userStorageWrapper;
    private UserCustomTelemetryAdapterProducer _userCustomTelemetryAdapterProducer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        _userCustomTelemetryAdapterProducer = new UserCustomTelemetryAdapterProducer(_customStorageWrapper, Mockito.mock(SDKMetadata.class));
        Field userCustomTelemetryAdapterProducer = UserCustomTelemetryAdapterProducer.class.getDeclaredField("_userStorageWrapper");
        userCustomTelemetryAdapterProducer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomTelemetryAdapterProducer, userCustomTelemetryAdapterProducer.getModifiers() & ~Modifier.FINAL);
        userCustomTelemetryAdapterProducer.set(_userCustomTelemetryAdapterProducer, _userStorageWrapper);

    }

    @Test
    public void testRecordLatency() {
        _userCustomTelemetryAdapterProducer.recordLatency(MethodEnum.TRACK, 10l);
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).increment(Mockito.anyString(), Mockito.anyLong());
    }

    @Test
    public void testRecordException() {
        _userCustomTelemetryAdapterProducer.recordException(MethodEnum.TRACK);
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).increment(Mockito.anyString(), Mockito.anyLong());
    }
}