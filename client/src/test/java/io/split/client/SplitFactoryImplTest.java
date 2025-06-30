package io.split.client;

import io.split.client.dtos.ProxyMTLSAuth;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.utils.FileTypeEnum;
import io.split.integrations.IntegrationsConfig;
import io.split.service.SplitHttpClientImpl;
import io.split.storages.enums.OperationMode;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import junit.framework.TestCase;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.BearerToken;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import pluggable.CustomStorageWrapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class SplitFactoryImplTest extends TestCase {
    public static final String API_KEY ="29013ionasdasd09u";
    public static final String ENDPOINT = "https://sdk.split-stage.io";
    public static final String EVENTS_ENDPOINT = "https://events.split-stage.io";
    public static final String AUTH_SERVICE = "https://auth.split-stage.io/api/auth";


    @Test
    public void testFactoryInstantiation() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationWithoutBlockUntilReady() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationIntegrationsConfig() throws Exception {
        IntegrationsConfig integrationsConfig = new IntegrationsConfig.Builder().build();
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(1000)
                .integrations(integrationsConfig)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationWithProxyCredentials() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT, EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(1000)
                .proxyPort(6060)
                .proxyUsername("test")
                .proxyPassword("password")
                .proxyHost(ENDPOINT)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);
        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());

        Field splitHttpClientField = SplitFactoryImpl.class.getDeclaredField("_splitHttpClient");
        splitHttpClientField.setAccessible(true);
        SplitHttpClientImpl client = (SplitHttpClientImpl) splitHttpClientField.get(splitFactory);

        Field httpClientField = SplitHttpClientImpl.class.getDeclaredField("_client");
        httpClientField.setAccessible(true);
        Class<?> InternalHttp = Class.forName("org.apache.hc.client5.http.impl.classic.InternalHttpClient");

        Field routePlannerField = InternalHttp.getDeclaredField("routePlanner");
        routePlannerField.setAccessible(true);
        DefaultProxyRoutePlanner routePlanner = (DefaultProxyRoutePlanner) routePlannerField.get(InternalHttp.cast(httpClientField.get(client)));

        Field proxyField = DefaultProxyRoutePlanner.class.getDeclaredField("proxy");
        proxyField.setAccessible(true);
        HttpHost proxy = (HttpHost) proxyField.get(routePlanner);

        Assert.assertEquals("http", proxy.getSchemeName());
        Assert.assertEquals(ENDPOINT, proxy.getHostName());
        Assert.assertEquals(6060, proxy.getPort());

        Field credentialsProviderField = InternalHttp.getDeclaredField("credentialsProvider");
        credentialsProviderField.setAccessible(true);
        BasicCredentialsProvider credentialsProvider = (BasicCredentialsProvider) credentialsProviderField.get(InternalHttp.cast(httpClientField.get(client)));

        Field credMapField = BasicCredentialsProvider.class.getDeclaredField("credMap");
        credMapField.setAccessible(true);
        ConcurrentHashMap<AuthScope, UsernamePasswordCredentials> credMap = (ConcurrentHashMap) credMapField.get(credentialsProvider);

        Assert.assertEquals("test", credMap.entrySet().stream().iterator().next().getValue().getUserName());
        assertNotNull(credMap.entrySet().stream().iterator().next().getValue().getUserPassword());

        splitFactory.destroy();
    }
/*
    @Test
    public void testFactoryInstantiationWithProxyToken() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT, EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(1000)
                .proxyPort(6060)
//                .proxyToken("123456789")
                .proxyHost(ENDPOINT)
                .build();
        SplitFactoryImpl splitFactory2 = new SplitFactoryImpl(API_KEY, splitClientConfig);
        assertNotNull(splitFactory2.client());
        assertNotNull(splitFactory2.manager());

        Field splitHttpClientField2 = SplitFactoryImpl.class.getDeclaredField("_splitHttpClient");
        splitHttpClientField2.setAccessible(true);
        SplitHttpClientImpl client2 = (SplitHttpClientImpl) splitHttpClientField2.get(splitFactory2);

        Field httpClientField2 = SplitHttpClientImpl.class.getDeclaredField("_client");
        httpClientField2.setAccessible(true);
        Class<?> InternalHttp2 = Class.forName("org.apache.hc.client5.http.impl.classic.InternalHttpClient");

        Field credentialsProviderField2 = InternalHttp2.getDeclaredField("credentialsProvider");
        credentialsProviderField2.setAccessible(true);
        BasicCredentialsProvider credentialsProvider2 = (BasicCredentialsProvider) credentialsProviderField2.get(InternalHttp2.cast(httpClientField2.get(client2)));

        Field credMapField2 = BasicCredentialsProvider.class.getDeclaredField("credMap");
        credMapField2.setAccessible(true);
        ConcurrentHashMap<AuthScope, BearerToken> credMap2 = (ConcurrentHashMap) credMapField2.get(credentialsProvider2);

        Assert.assertEquals("123456789", credMap2.entrySet().stream().iterator().next().getValue().getToken());

        splitFactory2.destroy();
    }
*/
    @Test
    public void testFactoryInstantiationWithProxyMtls() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(1000)
                .proxyPort(6060)
                .proxyScheme("https")
                .proxyMtlsAuth(new ProxyMTLSAuth.Builder().proxyP12File("src/test/resources/keyStore.p12").proxyP12FilePassKey("split").build())
                .proxyHost(ENDPOINT)
                .build();
        SplitFactoryImpl splitFactory3 = new SplitFactoryImpl(API_KEY, splitClientConfig);
        assertNotNull(splitFactory3.client());
        assertNotNull(splitFactory3.manager());

        Field splitHttpClientField3 = SplitFactoryImpl.class.getDeclaredField("_splitHttpClient");
        splitHttpClientField3.setAccessible(true);
        SplitHttpClientImpl client3 = (SplitHttpClientImpl) splitHttpClientField3.get(splitFactory3);

        Field httpClientField3 = SplitHttpClientImpl.class.getDeclaredField("_client");
        httpClientField3.setAccessible(true);
        Class<?> InternalHttp3 = Class.forName("org.apache.hc.client5.http.impl.classic.InternalHttpClient");

        Field connManagerField = InternalHttp3.getDeclaredField("connManager");
        connManagerField.setAccessible(true);
        PoolingHttpClientConnectionManager connManager = (PoolingHttpClientConnectionManager) connManagerField.get(InternalHttp3.cast(httpClientField3.get(client3)));

        Field connectionOperatorField = PoolingHttpClientConnectionManager.class.getDeclaredField("connectionOperator");
        connectionOperatorField.setAccessible(true);
        DefaultHttpClientConnectionOperator connectionOperator = (DefaultHttpClientConnectionOperator) connectionOperatorField.get(connManager);

        Field tlsSocketStrategyLookupField = DefaultHttpClientConnectionOperator.class.getDeclaredField("tlsSocketStrategyLookup");
        tlsSocketStrategyLookupField.setAccessible(true);
        Registry tlsSocketStrategyLookup = (Registry) tlsSocketStrategyLookupField.get(connectionOperator);

        Field mapField = Registry.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Class<?> map =  mapField.get(tlsSocketStrategyLookup).getClass();

        Class<?> value = ((ConcurrentHashMap) map.cast(mapField.get(tlsSocketStrategyLookup))).get("https").getClass();

        Field arg1Field = value.getDeclaredField("arg$1");
        arg1Field.setAccessible(true);
        Class<?> sslConnectionSocketFactory = arg1Field.get(((ConcurrentHashMap) map.cast(mapField.get(tlsSocketStrategyLookup))).get("https")).getClass();

        Field socketFactoryField = sslConnectionSocketFactory.getDeclaredField("socketFactory");
        socketFactoryField.setAccessible(true);
        Class<?> socketFactory = socketFactoryField.get(arg1Field.get(((ConcurrentHashMap) map.cast(mapField.get(tlsSocketStrategyLookup))).get("https"))).getClass();

        Field contextField = socketFactory.getDeclaredField("context");
        contextField.setAccessible(true);
        Class<?> context = Class.forName("sun.security.ssl.SSLContextImpl");

        Field keyManagerField = context.getDeclaredField("keyManager");
        keyManagerField.setAccessible(true);
        Class<?> keyManager = keyManagerField.get(contextField.get(socketFactoryField.get(arg1Field.get(((ConcurrentHashMap) map.cast(mapField.get(tlsSocketStrategyLookup))).get("https"))))).getClass();

        Field credentialsMapField = keyManager.getDeclaredField("credentialsMap");
        credentialsMapField.setAccessible(true);
        HashMap<String,Object> credentialsMap = (HashMap) credentialsMapField.get(keyManagerField.get(contextField.get(socketFactoryField.get(arg1Field.get(((ConcurrentHashMap) map.cast(mapField.get(tlsSocketStrategyLookup))).get("https"))))));

        assertNotNull(credentialsMap.get("1"));

        splitFactory3.destroy();
    }

    @Test
    public void testFactoryDestroy() throws Exception {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .build();

        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);
        //Before destroy we replace telemetryStorage via reflection.
        Field factoryDestroy = SplitFactoryImpl.class.getDeclaredField("_telemetryStorageProducer");
        factoryDestroy.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(factoryDestroy, factoryDestroy.getModifiers() & ~Modifier.FINAL);

        factoryDestroy.set(splitFactory, telemetryStorage);
        splitFactory.destroy();

        assertTrue(splitFactory.isDestroyed());
        Mockito.verify(telemetryStorage, Mockito.times(1)).recordSessionLength(Mockito.anyLong());
    }

    @Test
    public void testFactoryConsumerInstantiation() throws Exception {
        CustomStorageWrapper customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        UserStorageWrapper userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        when(userStorageWrapper.connect()).thenReturn(true);

        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(customStorageWrapper)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig, customStorageWrapper);
        Field splitFactoryImpl = SplitFactoryImpl.class.getDeclaredField("_userStorageWrapper");
        splitFactoryImpl.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(splitFactoryImpl, splitFactoryImpl.getModifiers() & ~Modifier.FINAL);
        splitFactoryImpl.set(splitFactory, userStorageWrapper);

        Field telemetryStorageProducer = SplitFactoryImpl.class.getDeclaredField("_telemetrySynchronizer");
        telemetryStorageProducer.setAccessible(true);
        modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(telemetryStorageProducer, telemetryStorageProducer.getModifiers() & ~Modifier.FINAL);
        telemetryStorageProducer.set(splitFactory, telemetrySynchronizer);
        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
        Thread.sleep(1500);
        Mockito.verify(userStorageWrapper, Mockito.times(1)).connect();
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeConfig(Mockito.anyObject(), Mockito.anyLong(), Mockito.anyObject(), Mockito.anyObject());
    }

    @Test
    public void testFactoryConsumerInstantiationRetryReadiness() throws Exception {
        CustomStorageWrapper customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        UserStorageWrapper userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        when(userStorageWrapper.connect()).thenReturn(false).thenReturn(true);
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(customStorageWrapper)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig, customStorageWrapper);
        Field splitFactoryImpl = SplitFactoryImpl.class.getDeclaredField("_userStorageWrapper");
        splitFactoryImpl.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(splitFactoryImpl, splitFactoryImpl.getModifiers() & ~Modifier.FINAL);
        splitFactoryImpl.set(splitFactory, userStorageWrapper);
        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
        Awaitility.await()
                .atMost(5L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertTrue(userStorageWrapper.connect()));

        Mockito.verify(userStorageWrapper, Mockito.times(2)).connect();
    }

    @Test
    public void testFactoryConsumerDestroy() throws NoSuchFieldException, URISyntaxException, IllegalAccessException {
        CustomStorageWrapper customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        UserStorageWrapper userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        when(userStorageWrapper.connect()).thenReturn(false).thenReturn(true);
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(customStorageWrapper)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig, customStorageWrapper);
        Field splitFactoryImpl = SplitFactoryImpl.class.getDeclaredField("_userStorageWrapper");
        splitFactoryImpl.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(splitFactoryImpl, splitFactoryImpl.getModifiers() & ~Modifier.FINAL);
        splitFactoryImpl.set(splitFactory, userStorageWrapper);
        splitFactory.destroy();

        assertTrue(splitFactory.isDestroyed());
        Mockito.verify(userStorageWrapper, Mockito.times(1)).disconnect();
    }

    @Test
    public void testLocalhostLegacy() throws URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof LegacyLocalhostSplitChangeFetcher);
    }

    @Test
    public void testLocalhostYaml() throws URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .splitFile("src/test/resources/split.yaml")
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof YamlLocalhostSplitChangeFetcher);
    }

    @Test
    public void testLocalhosJson() throws URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .splitFile("src/test/resources/split_init.json")
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof JsonLocalhostSplitChangeFetcher);
    }

    @Test
    public void testLocalhostYamlInputStream() throws URISyntaxException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException {
        InputStream inputStream = new FileInputStream("src/test/resources/split.yaml");
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .splitFile(inputStream, FileTypeEnum.YAML)
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof YamlLocalhostSplitChangeFetcher);
    }

    @Test
    public void testLocalhosJsonInputStream() throws URISyntaxException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException {
        InputStream inputStream = new FileInputStream("src/test/resources/split_init.json");
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .splitFile(inputStream, FileTypeEnum.JSON)
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof JsonLocalhostSplitChangeFetcher);
    }

    @Test
    public void testLocalhosJsonInputStreamNull() throws URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .splitFile(null, FileTypeEnum.JSON)
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof LegacyLocalhostSplitChangeFetcher);
    }

    @Test
    public void testLocalhosJsonInputStreamAndFileTypeNull() throws URISyntaxException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException {
        InputStream inputStream = new FileInputStream("src/test/resources/split_init.json");
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .splitFile(inputStream, null)
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof LegacyLocalhostSplitChangeFetcher);
    }

    @Test
    public void testLocalhosJsonInputStreamNullAndFileTypeNull() throws URISyntaxException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .splitFile(null, null)
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl("localhost", splitClientConfig);

        Method method = SplitFactoryImpl.class.getDeclaredMethod("createSplitChangeFetcher", SplitClientConfig.class);
        method.setAccessible(true);
        Object splitChangeFetcher = method.invoke(splitFactory, splitClientConfig);
        Assert.assertTrue(splitChangeFetcher instanceof LegacyLocalhostSplitChangeFetcher);
    }
}