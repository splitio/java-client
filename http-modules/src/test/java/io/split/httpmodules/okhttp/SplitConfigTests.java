package io.split.httpmodules.okhttp;

import okhttp3.OkHttpClient;
//import okhttp3.OkHttpClient.Builder;

public class SplitConfigTests {
    /*
    @Test
    public void checkExpectedAuthScheme() {
        OkHttpClient client = new Builder().build();
        OkHttpModuleImpl okHttpModuleImpl = new OkHttpModuleImpl(client, "qwerty");

        SplitClientConfig cfg = SplitClientConfig.builder()
                .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                .proxyKerberosPrincipalName("bilal@bilal")
                .proxyKerberosClient(okHttpModuleImpl)
                .build();
        Assert.assertEquals(ProxyAuthScheme.KERBEROS, cfg.proxyAuthScheme());
        Assert.assertEquals("bilal@bilal", cfg.proxyKerberosPrincipalName());
        Assert.assertEquals(okHttpModuleImpl, cfg.proxyKerberosClient());

        cfg = SplitClientConfig.builder()
                .build();
        Assert.assertEquals(null, cfg.proxyAuthScheme());
    }

    @Test(expected = IllegalStateException.class)
    public void testAuthSchemeWithoutClient() {
        SplitClientConfig.builder()
                .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                .proxyKerberosPrincipalName("bilal")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testAuthSchemeWithoutPrincipalName() {
        OkHttpClient client = new Builder().build();
        OkHttpModuleImpl okHttpModuleImpl = new OkHttpModuleImpl(client, "qwerty");

        SplitClientConfig.builder()
                .proxyAuthScheme(ProxyAuthScheme.KERBEROS)
                .proxyKerberosClient(okHttpModuleImpl)
                .build();
    }

     */

}
