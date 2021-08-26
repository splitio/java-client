package io.split.storages.pluggable.adapters;

import io.split.client.utils.Json;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class UserCustomSegmentAdapterProducerTest {

    private static final String SEGMENT_NAME = "SegmentName";
    private CustomStorageWrapper _customStorageWrapper;
    private SafeUserStorageWrapper _safeUserStorageWrapper;
    private UserCustomSegmentAdapterProducer _userCustomSegmentAdapterProducer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _safeUserStorageWrapper = Mockito.mock(SafeUserStorageWrapper.class);
        _userCustomSegmentAdapterProducer = new UserCustomSegmentAdapterProducer(_customStorageWrapper);
        Field userCustomSegmentAdapterProducer = UserCustomSegmentAdapterProducer.class.getDeclaredField("_safeUserStorageWrapper");
        userCustomSegmentAdapterProducer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomSegmentAdapterProducer, userCustomSegmentAdapterProducer.getModifiers() & ~Modifier.FINAL);
        userCustomSegmentAdapterProducer.set(_userCustomSegmentAdapterProducer, _safeUserStorageWrapper);
    }

    @Test
    public void testGetChangeNumber() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSegment(SEGMENT_NAME))).thenReturn(Json.toJson(120L));
        Assert.assertEquals(120L, _userCustomSegmentAdapterProducer.getChangeNumber(SEGMENT_NAME));
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testUpdateSegment() {
        _userCustomSegmentAdapterProducer.updateSegment(SEGMENT_NAME, new ArrayList<>(), new ArrayList<>(), 12L);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).addItems(Mockito.anyString(), Mockito.anyObject());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).removeItems(Mockito.anyString(), Mockito.anyObject());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetChangeNumber() {
        _userCustomSegmentAdapterProducer.setChangeNumber(SEGMENT_NAME, 1L);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
    }
}