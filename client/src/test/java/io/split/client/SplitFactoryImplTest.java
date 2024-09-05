package io.split.client;

import io.split.client.impressions.ImpressionsManager;
import io.split.client.utils.FileTypeEnum;
import io.split.client.utils.SDKMetadata;
import io.split.integrations.IntegrationsConfig;
import io.split.service.HttpAuthScheme;
import io.split.service.SplitHttpClient;
import io.split.service.SplitHttpClientKerberosImpl;
import io.split.storages.enums.OperationMode;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import pluggable.CustomStorageWrapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Interceptor;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SplitFactoryImpl.class)
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
    public void testFactoryInstantiationWithProxy() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
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
        Thread.sleep(2000);
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

    @Test
    public void testBuildKerberosClientParams() throws URISyntaxException, IOException {
        PowerMockito.mockStatic(SplitFactoryImpl.class);

        ArgumentCaptor<Proxy> proxyCaptor = ArgumentCaptor.forClass(Proxy.class);
        ArgumentCaptor<SplitClientConfig> configCaptor = ArgumentCaptor.forClass(SplitClientConfig.class);
        ArgumentCaptor< HttpLoggingInterceptor> logCaptor = ArgumentCaptor.forClass( HttpLoggingInterceptor.class);
        ArgumentCaptor<Authenticator> authCaptor = ArgumentCaptor.forClass(Authenticator.class);

        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .authScheme(HttpAuthScheme.KERBEROS)
                .kerberosPrincipalName("bilal@localhost")
                .proxyPort(6060)
                .proxyHost(ENDPOINT)
                .build();

        Map<String, String> kerberosOptions = new HashMap<String, String>();
        kerberosOptions.put("com.sun.security.auth.module.Krb5LoginModule", "required");
        kerberosOptions.put("refreshKrb5Config", "false");
        kerberosOptions.put("doNotPrompt", "false");
        kerberosOptions.put("useTicketCache", "true");
        BDDMockito.given(SplitFactoryImpl.getProxyAuthenticator(splitClientConfig, kerberosOptions))
            .willReturn(null);

        RequestDecorator requestDecorator = new RequestDecorator(null);
        SDKMetadata sdkmeta = new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
        PowerMockito.when(SplitFactoryImpl.buildSplitHttpClient("qwer",
                splitClientConfig,
                sdkmeta,
                requestDecorator)).thenCallRealMethod();

        SplitHttpClient splitHttpClient = SplitFactoryImpl.buildSplitHttpClient("qwer",
                splitClientConfig,
                sdkmeta,
                requestDecorator);

        PowerMockito.verifyStatic();
        SplitFactoryImpl.buildOkHttpClient(proxyCaptor.capture(), configCaptor.capture(),logCaptor.capture(), authCaptor.capture());

        Assert.assertTrue(splitHttpClient instanceof SplitHttpClientKerberosImpl);
        Assert.assertEquals("HTTP @ https://sdk.split-stage.io:6060", proxyCaptor.getValue().toString());
        Assert.assertTrue(logCaptor.getValue() instanceof okhttp3.logging.HttpLoggingInterceptor);
    }

    @Test
    public void testFactoryKerberosInstance() throws URISyntaxException, IOException {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        PowerMockito.stub(PowerMockito.method(SplitFactoryImpl.class, "buildOkHttpClient")).toReturn(okHttpClient);
        PowerMockito.stub(PowerMockito.method(SplitFactoryImpl.class, "getProxyAuthenticator")).toReturn(null);

        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .authScheme(HttpAuthScheme.KERBEROS)
                .kerberosPrincipalName("bilal@localhost")
                .proxyPort(6060)
                .proxyHost(ENDPOINT)
                .build();

        Map<String, String> kerberosOptions = new HashMap<String, String>();
        kerberosOptions.put("com.sun.security.auth.module.Krb5LoginModule", "required");
        kerberosOptions.put("refreshKrb5Config", "false");
        kerberosOptions.put("doNotPrompt", "false");
        kerberosOptions.put("useTicketCache", "true");

        RequestDecorator requestDecorator = new RequestDecorator(null);
        SDKMetadata sdkmeta = new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
        SplitHttpClient splitHttpClient = SplitFactoryImpl.buildSplitHttpClient("qwer",
                splitClientConfig,
                sdkmeta,
                requestDecorator);
        Assert.assertTrue(splitHttpClient instanceof SplitHttpClientKerberosImpl);
    }

    @Test
    public void testBuildOkHttpClient() {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .authScheme(HttpAuthScheme.KERBEROS)
                .kerberosPrincipalName("bilal@localhost")
                .proxyPort(6060)
                .proxyHost(ENDPOINT)
                .build();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("host", 8080));
        OkHttpClient okHttpClient = SplitFactoryImpl.buildOkHttpClient(proxy,
                splitClientConfig, loggingInterceptor,  Authenticator.NONE);
        assertEquals(Authenticator.NONE, okHttpClient.authenticator());
        assertEquals(proxy, okHttpClient.proxy());
        assertEquals(loggingInterceptor, okHttpClient.interceptors().get(0));
    }
}