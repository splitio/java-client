package io.split.httpmodules.okhttp;

import io.split.client.SplitClientConfig;
import org.junit.Assert;
import org.junit.Test;

public class SplitConfigTests {

    @Test
    public void checkExpectedAuthScheme() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .alternativeHTTPModule(OkHttpModule.builder()
                        .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                        .proxyAuthKerberosPrincipalName("bilal@bilal")
                        .proxyHost("some-proxy")
                        .proxyPort(3128)
                        .debugEnabled()
                        .build()
                )
                .streamingEnabled(false)
                .build();
        OkHttpModule module =  (OkHttpModule) cfg.alternativeHTTPModule();
        Assert.assertEquals(ProxyAuthScheme.KERBEROS, module.proxyAuthScheme());
        Assert.assertEquals("bilal@bilal", module.proxyKerberosPrincipalName());
        Assert.assertEquals("HTTP @ some-proxy:3128", module.proxy().toString());

        cfg = SplitClientConfig.builder()
                .build();
        Assert.assertEquals(null, cfg.alternativeHTTPModule());
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkStreamingEnabled() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .alternativeHTTPModule(OkHttpModule.builder()
                        .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                        .proxyAuthKerberosPrincipalName("bilal@bilal")
                        .proxyHost("some-proxy")
                        .proxyPort(3128)
                        .debugEnabled()
                        .build())
                .streamingEnabled(true)
                .build();
    }
}
