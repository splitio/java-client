package io.split.storages.pluggable.synchronizer;

import io.split.client.ApiKeyCounter;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.UniqueKeys;
import io.split.client.utils.SDKMetadata;
import io.split.storages.pluggable.domain.ConfigConsumer;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TelemetryConsumerSubmitterTest {
    private static final String FIRST_KEY = "KEY_1";
    private static final String SECOND_KEY = "KEY_2";

    @Test
    public void testSynchronizeConfig() {
        ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(SECOND_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(SECOND_KEY);
        TelemetryConsumerSubmitter telemetrySynchronizer = new TelemetryConsumerSubmitter(Mockito.mock(CustomStorageWrapper.class), new SDKMetadata("SDK 4.2.x", "22.215135.1", "testMachine"));
        SplitClientConfig splitClientConfig = SplitClientConfig.builder().build();
        ConfigConsumer config = telemetrySynchronizer.generateConfig(splitClientConfig, ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances(), Stream.of("tag1", "tag2").collect(Collectors.toList()));
        Assert.assertEquals(3, config.get_redundantFactories());
        Assert.assertEquals(2, config.get_tags().size());
    }

    @Test
    public void testTestSynchronizeConfig() throws Exception {
        UserStorageWrapper userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        TelemetryConsumerSubmitter telemetrySynchronizer = new TelemetryConsumerSubmitter(Mockito.mock(CustomStorageWrapper.class), new SDKMetadata("SDK 4.2.x", "22.215135.1", "testMachine"));
        SplitClientConfig splitClientConfig = SplitClientConfig.builder().build();
        Field telemetryConsumerSubmitterHolder = TelemetryConsumerSubmitter.class.getDeclaredField("_userStorageWrapper");
        telemetryConsumerSubmitterHolder.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(telemetryConsumerSubmitterHolder, telemetryConsumerSubmitterHolder.getModifiers() & ~Modifier.FINAL);
        telemetryConsumerSubmitterHolder.set(telemetrySynchronizer, userStorageWrapper);
        telemetrySynchronizer.synchronizeConfig(splitClientConfig, 10L, new HashMap<>(), new ArrayList<>());
        Mockito.verify(userStorageWrapper, Mockito.times(1)).hSet(Mockito.eq("SPLITIO.telemetry.init"), Mockito.eq("SDK 4.2.x/testMachine/22.215135.1"), Mockito.anyString());
    }

    @Test
    public void testTestSynchronizeUniqueKeys() throws NoSuchFieldException, IllegalAccessException {
        UserStorageWrapper userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        TelemetryConsumerSubmitter telemetrySynchronizer = new TelemetryConsumerSubmitter(Mockito.mock(CustomStorageWrapper.class), new SDKMetadata("SDK 4.2.x", "22.215135.1", "testMachine"));
        Field telemetryConsumerSubmitterHolder = TelemetryConsumerSubmitter.class.getDeclaredField("_userStorageWrapper");
        telemetryConsumerSubmitterHolder.setAccessible(true);
        telemetryConsumerSubmitterHolder.set(telemetrySynchronizer, userStorageWrapper);

        List<String> keys = new ArrayList<>();
        keys.add("key-1");
        keys.add("key-2");
        List<UniqueKeys.UniqueKey> uniqueKeys = new ArrayList<>();
        uniqueKeys.add(new UniqueKeys.UniqueKey("feature-1", keys));
        UniqueKeys uniqueKeysToSend = new UniqueKeys(uniqueKeys);

        telemetrySynchronizer.synchronizeUniqueKeys(uniqueKeysToSend);
        List<String> uniqueKeysJson = new ArrayList<>(Collections.singletonList("{\"keys\":[{\"f\":\"feature-1\",\"ks\":[\"key-1\",\"key-2\"]}]}"));
        Mockito.verify(userStorageWrapper).pushItems(Mockito.eq("SPLITIO.uniquekeys"), Mockito.eq(uniqueKeysJson));
    }
}