package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Event;
import io.split.storages.pluggable.CustomStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UserCustomEventAdapterProducerTest{
    private CustomStorageWrapper _customStorageWrapper;
    private UserCustomEventAdapterProducer _eventAdapterProducer;

    @Before
    public void setUp() {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _eventAdapterProducer = new UserCustomEventAdapterProducer(_customStorageWrapper);
    }

    @Test
    public void testTrack() {
        Event event = new Event();
        Assert.assertTrue(_eventAdapterProducer.track(event,1));
    }

}