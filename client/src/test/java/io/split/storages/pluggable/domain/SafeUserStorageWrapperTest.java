package io.split.storages.pluggable.domain;

import io.split.storages.pluggable.CustomStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SafeUserStorageWrapperTest{

    private static final String KEY = "KEY";
    private static final String RESPONSE = "Response";
    private static final String ITEM = "Item";
    private CustomStorageWrapper _customStorageWrapper;
    private SafeUserStorageWrapper _safeUserStorageWrapper;
    private Logger _log;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _log = Mockito.mock(Logger.class);
        _safeUserStorageWrapper = new SafeUserStorageWrapper(_customStorageWrapper);
        Field safeUserStorageWrapper = SafeUserStorageWrapper.class.getDeclaredField("_log");
        safeUserStorageWrapper.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(safeUserStorageWrapper, safeUserStorageWrapper.getModifiers() & ~Modifier.FINAL);
        safeUserStorageWrapper.set(_safeUserStorageWrapper, _log);
    }

    @Test
    public void testGet() throws Exception {
        Mockito.when(_customStorageWrapper.get(Mockito.anyString())).thenReturn(RESPONSE);
        String result = _safeUserStorageWrapper.get(KEY);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetException() throws Exception {
        Mockito.when(_customStorageWrapper.get(Mockito.anyString())).thenThrow(Exception.class);
        String result = _safeUserStorageWrapper.get(KEY);
        Assert.assertNull(result);
    }

    @Test
    public void testGetMany() throws Exception {
        Mockito.when(_customStorageWrapper.getMany(Mockito.anyObject())).thenReturn(RESPONSE);
        String result = _safeUserStorageWrapper.getMany(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetManyException() throws Exception {
        Mockito.when(_customStorageWrapper.getMany(Mockito.anyObject())).thenThrow(Exception.class);
        String result = _safeUserStorageWrapper.getMany(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNull(result);
    }

    @Test
    public void testSet() {

        _safeUserStorageWrapper.set(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testSetException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).set(Mockito.anyString(), Mockito.anyString());
        _safeUserStorageWrapper.set(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testDelete() {
        _safeUserStorageWrapper.delete(Stream.of(KEY).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testDeleteException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).delete(Mockito.anyObject());
        _safeUserStorageWrapper.delete(Stream.of(KEY).collect(Collectors.toList()));
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testGetByPrefix() throws Exception {
        Mockito.when(_customStorageWrapper.getByPrefix(Mockito.anyString())).thenReturn(RESPONSE);
        String result = _safeUserStorageWrapper.getByPrefix(KEY);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetByPrefixException() throws Exception {
        Mockito.when(_customStorageWrapper.getByPrefix(Mockito.anyString())).thenThrow(Exception.class);
        String result = _safeUserStorageWrapper.getByPrefix(KEY);
        Assert.assertNull(result);
    }

    @Test
    public void testGetAndSet() throws Exception {
        Mockito.when(_customStorageWrapper.getAndSet(Mockito.anyString(), Mockito.anyObject())).thenReturn(RESPONSE);
        String result = _safeUserStorageWrapper.getAndSet(KEY, ITEM);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetAndSetException() throws Exception {
        Mockito.when(_customStorageWrapper.getAndSet(Mockito.anyString(), Mockito.anyObject())).thenThrow(Exception.class);
        String result = _safeUserStorageWrapper.getAndSet(KEY, ITEM);
        Assert.assertNull(result);
    }

    @Test
    public void testGetKeysByPrefix() throws Exception {
        Mockito.when(_customStorageWrapper.getKeysByPrefix(Mockito.anyString())).thenReturn(RESPONSE);
        String result = _safeUserStorageWrapper.getKeysByPrefix(KEY);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetKeysByPrefixException() throws Exception {
        Mockito.when(_customStorageWrapper.getKeysByPrefix(Mockito.anyString())).thenThrow(Exception.class);
        String result = _safeUserStorageWrapper.getKeysByPrefix(KEY);
        Assert.assertNull(result);
    }

    @Test
    public void testIncrement() {
        _safeUserStorageWrapper.increment(KEY, 1L);
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testIncrementException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).increment(Mockito.anyString(), Mockito.anyLong());
        _safeUserStorageWrapper.increment(KEY, 1L);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testDecrement() {
        _safeUserStorageWrapper.decrement(KEY, 1L);
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testDecrementException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).decrement(Mockito.anyString(), Mockito.anyLong());
        _safeUserStorageWrapper.decrement(KEY, 1L);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testPushItems() {
        _safeUserStorageWrapper.pushItems(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testPushItemsException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).pushItems(Mockito.anyString(), Mockito.anyString());
        _safeUserStorageWrapper.pushItems(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testPopItems() throws Exception {
        Mockito.when(_customStorageWrapper.popItems(Mockito.anyString(), Mockito.anyLong())).thenReturn(RESPONSE);
        String result = _safeUserStorageWrapper.popItems(KEY, 1L);
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testPopItemsException() throws Exception {
        Mockito.when(_customStorageWrapper.popItems(Mockito.anyString(), Mockito.anyLong())).thenThrow(Exception.class);
        String result = _safeUserStorageWrapper.popItems(KEY, 1L);
        Assert.assertNull(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testGetItemsCount(){
        long response = 2L;
        Mockito.when(_customStorageWrapper.getItemsCount(Mockito.anyString())).thenReturn(response);
        long result = _safeUserStorageWrapper.getItemsCount(KEY);
        Assert.assertEquals(response, result);
    }

    @Test
    public void testGetItemsCountException(){
        Mockito.when(_customStorageWrapper.getItemsCount(Mockito.anyString())).thenThrow(Exception.class);
        long result = _safeUserStorageWrapper.getItemsCount(KEY);
        Assert.assertEquals(0L, result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testItemContains() throws Exception {
        Mockito.when(_customStorageWrapper.itemContains(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        boolean result = _safeUserStorageWrapper.itemContains(KEY, ITEM);
        Assert.assertTrue(result);
    }

    @Test
    public void testItemContainsException() throws Exception {
        Mockito.when(_customStorageWrapper.itemContains(Mockito.anyString(), Mockito.anyString())).thenThrow(Exception.class);
        boolean result = _safeUserStorageWrapper.itemContains(KEY, ITEM);
        Assert.assertFalse(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testAddItems() {
        _safeUserStorageWrapper.addItems(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testAddItemsException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).addItems(Mockito.anyString(), Mockito.anyString());
        _safeUserStorageWrapper.addItems(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testRemoveItems() {
        _safeUserStorageWrapper.removeItems(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(0)).error(Mockito.anyString());
    }

    @Test
    public void testRemoveItemsException() throws Exception {
        Mockito.doThrow(Exception.class).when(_customStorageWrapper).removeItems(Mockito.anyString(), Mockito.anyString());
        _safeUserStorageWrapper.removeItems(KEY, ITEM);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }

    @Test
    public void testGetItems() throws Exception {
        Mockito.when(_customStorageWrapper.getItems(Mockito.anyObject())).thenReturn(RESPONSE);
        String result = _safeUserStorageWrapper.getItems(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNotNull(result);
        Assert.assertEquals(RESPONSE, result);
    }

    @Test
    public void testGetItemsException() throws Exception {
        Mockito.when(_customStorageWrapper.getItems(Mockito.anyObject())).thenThrow(Exception.class);
        String result = _safeUserStorageWrapper.getItems(Stream.of(KEY).collect(Collectors.toList()));
        Assert.assertNull(result);
        Mockito.verify(_log, Mockito.times(1)).error(Mockito.anyString());
    }



}