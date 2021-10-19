package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Event;
import io.split.client.dtos.Metadata;
import io.split.storages.pluggable.CustomStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class UserCustomEventAdapterProducerTest{
    private CustomStorageWrapper _customStorageWrapper;
    private UserCustomEventAdapterProducer _eventAdapterProducer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _eventAdapterProducer = new UserCustomEventAdapterProducer(_customStorageWrapper, Mockito.mock(Metadata.class));
        Metadata metadata = new Metadata(true, "SDK-version");
        Field userCustomMetadata = UserCustomEventAdapterProducer.class.getDeclaredField("_metadata");
        userCustomMetadata.setAccessible(true);
        Field modifiersFieldMetadata = Field.class.getDeclaredField("modifiers");
        modifiersFieldMetadata.setAccessible(true);
        modifiersFieldMetadata.setInt(userCustomMetadata, userCustomMetadata.getModifiers() & ~Modifier.FINAL);
        userCustomMetadata.set(_eventAdapterProducer, metadata);
    }

    @Test
    public void testTrack() {
        Event event = new Event();
        Assert.assertTrue(_eventAdapterProducer.track(event,1));
    }

}