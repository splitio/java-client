package io.split.httpmodules.okhttp;

import io.split.client.SplitFactoryImpl;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.*;
import okhttp3.HttpUrl;
import okhttp3.Headers;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SplitFactoryImpl.class)
public class SplitFactoryTests {
    /*
    public static final String ENDPOINT = "https://sdk.split-stage.io";

    @Test
    public void testBuildKerberosClientParams() throws URISyntaxException, IOException {
        PowerMockito.mockStatic(SplitFactoryImpl.class);
        PowerMockito.mockStatic(OkHttpModule.class);

        ArgumentCaptor<Proxy> proxyCaptor = ArgumentCaptor.forClass(Proxy.class);
        ArgumentCaptor<SplitClientConfig> configCaptor = ArgumentCaptor.forClass(SplitClientConfig.class);
        ArgumentCaptor< HttpLoggingInterceptor> logCaptor = ArgumentCaptor.forClass( HttpLoggingInterceptor.class);
        ArgumentCaptor<Authenticator> authCaptor = ArgumentCaptor.forClass(Authenticator.class);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ENDPOINT, 6060));
        OkHttpClient client = OkHttpModule.buildOkHttpClient(proxy, "bilal@localhost", true, 0, 0)
        OkHttpModuleImpl okHttpModuleImpl = new OkHttpModuleImpl(client, "qwerty");

        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                .proxyKerberosPrincipalName("bilal@localhost")
                .proxyKerberosClient(okHttpModuleImpl)
                .build();

        Map<String, String> kerberosOptions = new HashMap<String, String>();
        kerberosOptions.put("com.sun.security.auth.module.Krb5LoginModule", "required");
        kerberosOptions.put("refreshKrb5Config", "false");
        kerberosOptions.put("doNotPrompt", "false");
        kerberosOptions.put("useTicketCache", "true");
        BDDMockito.given(OkHttpModule.getProxyAuthenticator("bilal@localhost", kerberosOptions))
                .willReturn(null);

        RequestDecorator requestDecorator = new RequestDecorator(null);
        SDKMetadata sdkmeta = new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");

        PowerMockito.verifyStatic();
        SplitFactoryImpl.buildOkHttpClient(proxyCaptor.capture(), configCaptor.capture(),logCaptor.capture(), authCaptor.capture());

        Assert.assertEquals("HTTP @ https://sdk.split-stage.io:6060", proxyCaptor.getValue().toString());
        Assert.assertTrue(logCaptor.getValue() instanceof okhttp3.logging.HttpLoggingInterceptor);
    }

    @Test
    public void testFactoryKerberosInstance() throws URISyntaxException, IOException {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        PowerMockito.stub(PowerMockito.method(OkHttpModule.class, "buildOkHttpClient")).toReturn(okHttpClient);
        PowerMockito.stub(PowerMockito.method(OkHttpModule.class, "getProxyAuthenticator")).toReturn(null);

        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                .proxyKerberosPrincipalName("bilal@localhost")
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
        Assert.assertTrue(splitHttpClient instanceof OkHttpModuleImpl);
    }

    @Test
    public void testBuildOkHttpClient() {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                .proxyKerberosPrincipalName("bilal@localhost")
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

     */

}
