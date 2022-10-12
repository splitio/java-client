package io.split.storages.pluggable.adapters;

import io.split.client.utils.Json;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserCustomSegmentAdapterConsumerTest {

    private static final String SEGMENT_NAME = "SegmentName";
    private CustomStorageWrapper _customStorageWrapper;
    private UserStorageWrapper _userStorageWrapper;
    private UserCustomSegmentAdapterConsumer _userCustomSegmentAdapterConsumer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        _userCustomSegmentAdapterConsumer = new UserCustomSegmentAdapterConsumer(_customStorageWrapper);
        Field userCustomSegmentAdapterConsumer = UserCustomSegmentAdapterConsumer.class.getDeclaredField("_userStorageWrapper");
        userCustomSegmentAdapterConsumer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomSegmentAdapterConsumer, userCustomSegmentAdapterConsumer.getModifiers() & ~Modifier.FINAL);
        userCustomSegmentAdapterConsumer.set(_userCustomSegmentAdapterConsumer, _userStorageWrapper);
    }

    @Test
    public void testGetChangeNumber() {
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildSegment(SEGMENT_NAME))).thenReturn(Json.toJson(120L));
        Assert.assertEquals(120L, _userCustomSegmentAdapterConsumer.getChangeNumber(SEGMENT_NAME));
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testIsInSegment() {
        Mockito.when(_userStorageWrapper.itemContains(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Assert.assertTrue(_userCustomSegmentAdapterConsumer.isInSegment(SEGMENT_NAME, "item"));
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).itemContains(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testGetSegmentCount() {
        Mockito.when(_userStorageWrapper.getKeysByPrefix(Mockito.anyString())).thenReturn(Collections.singleton(SEGMENT_NAME));
        Assert.assertEquals(1, _userCustomSegmentAdapterConsumer.getSegmentCount());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).getKeysByPrefix(Mockito.anyString());
    }

    @Test
    public void testGetKeyCount() {
        Mockito.when(_userStorageWrapper.getKeysByPrefix(Mockito.anyString())).thenReturn(Stream.of(SEGMENT_NAME, SEGMENT_NAME+"2").collect(Collectors.toSet()));
        Mockito.when(_userStorageWrapper.getItemsCount(Mockito.anyString())).thenReturn(1L).thenReturn(3L);
        Assert.assertEquals(4, _userCustomSegmentAdapterConsumer.getKeyCount());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).getKeysByPrefix(Mockito.anyString());
        Mockito.verify(_userStorageWrapper, Mockito.times(2)).getItemsCount(Mockito.anyString());
    }

    @Test
    public void testGetKeyCountNullResponse() {
        Mockito.when(_userStorageWrapper.getKeysByPrefix(Mockito.anyString())).thenReturn(null);
        Mockito.when(_userStorageWrapper.getItemsCount(Mockito.anyString())).thenReturn(1L).thenReturn(3L);
        Assert.assertEquals(0, _userCustomSegmentAdapterConsumer.getKeyCount());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).getKeysByPrefix(Mockito.anyString());
        Mockito.verify(_userStorageWrapper, Mockito.times(0)).getItemsCount(Mockito.anyString());
    }
}