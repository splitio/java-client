package io.split.httpmodules.okhttp;

import io.split.client.*;
import io.split.client.utils.SDKMetadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest(OkHttpModule.class)
public class SplitFactoryTests {
    @Test
    public void testFactoryCreatingClient() throws Exception {
        OkHttpClientImpl mockclient = mock(OkHttpClientImpl.class);
        AtomicBoolean argsCaptured = new AtomicBoolean(false);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("some-proxy", 3128));
        String apiToken = "qwerty";

        whenNew(OkHttpClientImpl.class).withAnyArguments()
                .then((Answer<OkHttpClientImpl>) invocationOnMock -> {
                            assertThat("qwerty", is(equalTo((String) invocationOnMock.getArguments()[0])));
                            assertThat((SDKMetadata) invocationOnMock.getArguments()[1], instanceOf(SDKMetadata.class));
                            assertThat(proxy, is(equalTo((Proxy) invocationOnMock.getArguments()[2])));
                            assertThat("bilal@bilal", is(equalTo((String) invocationOnMock.getArguments()[3])));
                            assertThat(false, is(equalTo((Boolean) invocationOnMock.getArguments()[4])));
                            assertThat(11000, is(equalTo((Integer) invocationOnMock.getArguments()[5])));
                            assertThat(12000, is(equalTo((Integer) invocationOnMock.getArguments()[6])));
                            assertThat((RequestDecorator) invocationOnMock.getArguments()[7], instanceOf(RequestDecorator.class));
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

        SplitClientConfig cfg = SplitClientConfig.builder()
                .alternativeHTTPModule(module)
                .streamingEnabled(false)
                .build();

        SplitFactoryImpl factory = (SplitFactoryImpl) SplitFactoryBuilder.build(apiToken, cfg);

//        module.createClient(apiToken, sdkMetadata, requestDecorator);
        assertThat(true, is(equalTo(argsCaptured.get())));
    }
}
