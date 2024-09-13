package io.split.httpmodules.okhttp;

import io.split.client.RequestDecorator;
import io.split.client.utils.SDKMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest(OkHttpModule.class)
public class OkHttpModuleTests {
    @Test
    public void checkProxySettings() {
        OkHttpModule module = OkHttpModule.builder()
                        .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                        .proxyAuthKerberosPrincipalName("bilal@bilal")
                        .proxyHost("some-proxy")
                        .proxyPort(3128)
                        .build();
        Assert.assertEquals(ProxyAuthScheme.KERBEROS, module.proxyAuthScheme());
        Assert.assertEquals("bilal@bilal", module.proxyKerberosPrincipalName());
        Assert.assertEquals("HTTP @ some-proxy:3128", module.proxy().toString());
    }

    @Test
    public void checkDebugLog() {
        OkHttpModule module = OkHttpModule.builder()
                .debugEnabled()
                .build();
        Assert.assertEquals(true, module.debugEnabled());

        module = OkHttpModule.builder()
                .build();
        Assert.assertEquals(false, module.debugEnabled());
    }

    @Test
    public void checkTimeouts() {
        OkHttpModule module = OkHttpModule.builder()
                .build();
        Assert.assertEquals(15000, (int) module.connectionTimeout());
        Assert.assertEquals(15000, (int) module.readTimeout());

        module = OkHttpModule.builder()
                .connectionTimeout(13000)
                .readTimeout(14000)
                .build();
        Assert.assertEquals(13000, (int) module.connectionTimeout());
        Assert.assertEquals(14000, (int) module.readTimeout());

        module = OkHttpModule.builder()
                .connectionTimeout(-1)
                .readTimeout(-10)
                .build();
        Assert.assertEquals(15000, (int) module.connectionTimeout());
        Assert.assertEquals(15000, (int) module.readTimeout());
    }

    @Test
    public void testCreateClient() throws Exception {
        OkHttpClientImpl mockclient = mock(OkHttpClientImpl.class);
        AtomicBoolean argsCaptured = new AtomicBoolean(false);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("some-proxy", 3128));
        String apiToken = "qwerty";
        SDKMetadata sdkMetadata = new SDKMetadata("1.1.1", "ip", "name");
        RequestDecorator requestDecorator = new RequestDecorator(null);

        whenNew(OkHttpClientImpl.class).withAnyArguments()
                .then((Answer<OkHttpClientImpl>) invocationOnMock -> {
                        assertThat("qwerty", is(equalTo((String) invocationOnMock.getArguments()[0])));
                        assertThat(sdkMetadata, is(equalTo((SDKMetadata) invocationOnMock.getArguments()[1])));
                        assertThat(proxy, is(equalTo((Proxy) invocationOnMock.getArguments()[2])));
                        assertThat("bilal@bilal", is(equalTo((String) invocationOnMock.getArguments()[3])));
                        assertThat(false, is(equalTo((Boolean) invocationOnMock.getArguments()[4])));
                        assertThat(11000, is(equalTo((Integer) invocationOnMock.getArguments()[5])));
                        assertThat(12000, is(equalTo((Integer) invocationOnMock.getArguments()[6])));
                        assertThat(requestDecorator, is(equalTo((RequestDecorator) invocationOnMock.getArguments()[7])));
                        argsCaptured.set(true);
                        return mockclient;
                    }
                );

        OkHttpModule module = OkHttpModule.builder()
                .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                .proxyAuthKerberosPrincipalName("bilal@bilal")
                .proxyHost("some-proxy")
                .proxyPort(3128)
                .connectionTimeout(12000)
                .readTimeout(11000)
                .build();

        module.createClient(apiToken, sdkMetadata, requestDecorator);
        assertThat(true, is(equalTo(argsCaptured.get())));
    }
}
