package io.split.storages.pluggable.adapters;

import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.Metadata;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserCustomImpressionAdapterProducerTest {

    private CustomStorageWrapper _customStorageWrapper;
    private UserCustomImpressionAdapterProducer _impressionAdapterProducer;
    private UserStorageWrapper _userStorageWrapper;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        _impressionAdapterProducer = new UserCustomImpressionAdapterProducer(_customStorageWrapper, Mockito.mock(Metadata.class));
        Field userCustomImpressionAdapterProducer = UserCustomImpressionAdapterProducer.class.getDeclaredField("_userStorageWrapper");
        userCustomImpressionAdapterProducer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomImpressionAdapterProducer, userCustomImpressionAdapterProducer.getModifiers() & ~Modifier.FINAL);
        userCustomImpressionAdapterProducer.set(_impressionAdapterProducer, _userStorageWrapper);
        Metadata metadata = new Metadata(true, "SDK-version");
        Field userCustomMetadata = UserCustomImpressionAdapterProducer.class.getDeclaredField("_metadata");
        userCustomMetadata.setAccessible(true);
        Field modifiersFieldMetadata = Field.class.getDeclaredField("modifiers");
        modifiersFieldMetadata.setAccessible(true);
        modifiersFieldMetadata.setInt(userCustomMetadata, userCustomMetadata.getModifiers() & ~Modifier.FINAL);
        userCustomMetadata.set(_impressionAdapterProducer, metadata);
    }

    @Test
    public void testPut() {
        KeyImpression keyImpression = new KeyImpression();
        Mockito.when(_userStorageWrapper.pushItems(Mockito.anyString(), Mockito.anyObject())).thenReturn(1L);
        Assert.assertEquals(1L, _impressionAdapterProducer.put(Stream.of(keyImpression).collect(Collectors.toList())));
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).pushItems(Mockito.anyString(), Mockito.anyObject());
    }

    @Test
    public void testPutMany() {
        KeyImpression keyImpression = new KeyImpression();
        Mockito.when(_userStorageWrapper.pushItems(Mockito.anyString(), Mockito.anyObject())).thenReturn(1L);
        Assert.assertEquals(1L, _impressionAdapterProducer.put(Stream.of(keyImpression).collect(Collectors.toList())));
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).pushItems(Mockito.anyString(), Mockito.anyObject());
    }

}