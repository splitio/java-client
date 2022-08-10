package io.split.storages.pluggable.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserStorageWrapperTest {

    private static final String KEY = "KEY";
    private static final String RESPONSE = "Response";
    private static final String ITEM = "Item";
    private CustomStorageWrapper _customStorageWrapper;
    private userStorageWrapper _userStorageWrapper;
    private Logger _log;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _log = Mockito.mock(Logger.class);
        _userStorageWrapper = new userStorageWrapper(_customStorageWrapper);
        Field userStorageWrapper = userStorageWrapper.class.getDeclaredField("_log");
        userStorageWrapper.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userStorageWrapper, userStorageWrapper.getModifiers() & ~Modifier.FINAL);
        userStorageWrapper.set(_userStorageWrapper, _log);
    }

    @Test
    public void testGet() throws Exception {
        Mockito.when(_customStorageWrapper.get(Mockito.anyString())).thenReturn(RESPONSE);
        String result = _userStorageWrapper.get(KEY);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetException() throws Exception {
        Mockito.when(_customStorageWrapper.get(Mockito.anyString())).thenThrow(Exception.class);
        String result = _userStorageWrapper.get(KEY);
        Assert.assertNull(result);
    }

    @Test
    public void testGetMany() throws Exception {
        Mockito.when(_customStorageWrapper.getMany(Mockito.anyObject())).thenReturn(Stream.of(RESPONSE).collect(Collectors.toList()));
        List<String> result = _userStorageWrapper.getMany(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(RESPONSE, result.get(0));
    }

    @Test
    public void testGetManyException() throws Exception {
        Mockito.when(_customStorageWrapper.getMany(Mockito.anyObject())).thenThrow(Exception.class);
        List<String> result = _userStorageWrapper.getMany(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNull(result);
    }

    @Test
    public void testSet() {

        _userStorageWrapper.set(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testSetException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).set(Mockito.anyString(), Mockito.anyString());
        _userStorageWrapper.set(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testDelete() {
        _userStorageWrapper.delete(Stream.of(KEY).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testDeleteException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).delete(Mockito.anyObject());
        _userStorageWrapper.delete(Stream.of(KEY).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testGetAndSet() throws Exception {
        Mockito.when(_customStorageWrapper.getAndSet(Mockito.anyString(), Mockito.anyObject())).thenReturn(RESPONSE);
        String result = _userStorageWrapper.getAndSet(KEY, ITEM);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetAndSetException() throws Exception {
        Mockito.when(_customStorageWrapper.getAndSet(Mockito.anyString(), Mockito.anyObject())).thenThrow(Exception.class);
        String result = _userStorageWrapper.getAndSet(KEY, ITEM);
        Assert.assertNull(result);
    }

    @Test
    public void testGetKeysByPrefix() throws Exception {
        Set<String> response =new HashSet<>();
        response.add(RESPONSE);
        Mockito.when(_customStorageWrapper.getKeysByPrefix(Mockito.anyString())).thenReturn(response);
        Set<String> result = _userStorageWrapper.getKeysByPrefix(KEY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains(RESPONSE));
    }

    @Test
    public void testGetKeysByPrefixException() throws Exception {
        Mockito.when(_customStorageWrapper.getKeysByPrefix(Mockito.anyString())).thenThrow(Exception.class);
        Set<String> result = _userStorageWrapper.getKeysByPrefix(KEY);
        Assert.assertNull(result);
    }

    @Test
    public void testIncrement() throws Exception {
        long response = 2L;
        Mockito.when(_customStorageWrapper.increment(Mockito.anyString(), Mockito.anyLong())).thenReturn(response);
        long result = _userStorageWrapper.increment(KEY, 1);
        Assert.assertEquals(response, result);
    }

    @Test
    public void testIncrementException() throws Exception {
        Mockito.when(_customStorageWrapper.increment(Mockito.anyString(), Mockito.anyLong())).thenThrow(Exception.class);
        long result = _userStorageWrapper.increment(KEY, 1);
        Assert.assertEquals(0L, result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testDecrement() throws Exception {
        long response = 2L;
        Mockito.when(_customStorageWrapper.decrement(Mockito.anyString(), Mockito.anyLong())).thenReturn(response);
        long result = _userStorageWrapper.decrement(KEY, 1);
        Assert.assertEquals(response, result);
    }

    @Test
    public void testDecrementException() throws Exception {
        Mockito.when(_customStorageWrapper.decrement(Mockito.anyString(), Mockito.anyLong())).thenThrow(Exception.class);
        long result = _userStorageWrapper.decrement(KEY, 1);
        Assert.assertEquals(0L, result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testPushItems() {
        _userStorageWrapper.pushItems(KEY, Stream.of(ITEM).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testPushItemsException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).pushItems(Mockito.anyString(), Mockito.anyObject());
        _userStorageWrapper.pushItems(KEY, Stream.of(ITEM).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testPopItems() throws Exception {
        Mockito.when(_customStorageWrapper.popItems(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(Stream.of(RESPONSE).collect(Collectors.toList()));
        List<String> result = _userStorageWrapper.popItems(KEY, 1L);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result.get(0));
    }

    @Test
    public void testPopItemsException() throws Exception {
        Mockito.when(_customStorageWrapper.popItems(Mockito.anyString(), Mockito.anyLong())).thenThrow(Exception.class);
        List<String> result = _userStorageWrapper.popItems(KEY, 1L);
        Assert.assertNull(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testGetItemsCount() throws Exception {
        long response = 2L;
        Mockito.when(_customStorageWrapper.getItemsCount(Mockito.anyString())).thenReturn(response);
        long result = _userStorageWrapper.getItemsCount(KEY);
        Assert.assertEquals(response, result);
    }

    @Test
    public void testGetItemsCountException() throws Exception {
        Mockito.when(_customStorageWrapper.getItemsCount(Mockito.anyString())).thenThrow(Exception.class);
        long result = _userStorageWrapper.getItemsCount(KEY);
        Assert.assertEquals(-1L, result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testItemContains() throws Exception {
        Mockito.when(_customStorageWrapper.itemContains(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        boolean result = _userStorageWrapper.itemContains(KEY, ITEM);
        Assert.assertTrue(result);
    }

    @Test
    public void testItemContainsException() throws Exception {
        Mockito.when(_customStorageWrapper.itemContains(Mockito.anyString(), Mockito.anyString())).thenThrow(Exception.class);
        boolean result = _userStorageWrapper.itemContains(KEY, ITEM);
        Assert.assertFalse(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testAddItems() {
        _userStorageWrapper.addItems(KEY, Stream.of(ITEM).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testAddItemsException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).addItems(Mockito.anyString(), Mockito.anyObject());
        _userStorageWrapper.addItems(KEY, Stream.of(ITEM).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testRemoveItems() {
        _userStorageWrapper.removeItems(KEY, Stream.of(ITEM).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testRemoveItemsException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).removeItems(Mockito.anyString(), Mockito.anyObject());
        _userStorageWrapper.removeItems(KEY, Stream.of(ITEM).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testGetItems() throws Exception {
        Mockito.when(_customStorageWrapper.getItems(Mockito.anyObject())).thenReturn(Stream.of(RESPONSE).collect(Collectors.toList()));
        List<String> result = _userStorageWrapper.getItems(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result.get(0));
    }

    @Test
    public void testGetItemsException() throws Exception {
        Mockito.when(_customStorageWrapper.getItems(Mockito.anyObject())).thenThrow(Exception.class);
        List<String> result = _userStorageWrapper.getItems(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNull(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testConnect() throws Exception {
        Mockito.when(_customStorageWrapper.connect()).thenReturn(true);
        boolean result = _userStorageWrapper.connect();
        Assert.assertTrue(result);
    }

    @Test
    public void testConnectFailed() throws Exception {
        Mockito.when(_customStorageWrapper.connect()).thenThrow(Exception.class);
        boolean result = _userStorageWrapper.connect();
        Assert.assertFalse(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testDisconnect() throws Exception {
        Mockito.when(_customStorageWrapper.disconnect()).thenReturn(true);
        boolean result = _userStorageWrapper.disconnect();
        Assert.assertTrue(result);
    }

    @Test
    public void testDisconnectFailed() throws Exception {
        Mockito.when(_customStorageWrapper.disconnect()).thenThrow(Exception.class);
        boolean result = _userStorageWrapper.disconnect();
        Assert.assertFalse(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }
}
