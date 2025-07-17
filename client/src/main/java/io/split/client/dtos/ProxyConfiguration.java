package io.split.client.dtos;

import org.apache.hc.core5.http.HttpHost;

import java.net.MalformedURLException;
import java.net.URL;

public class ProxyConfiguration {
    private final HttpHost _proxyHost;
    private ProxyCredentialsProvider _provider;
    private final String _p12File;
    private final String _passKey;

    private ProxyConfiguration(HttpHost proxyHost,
                               ProxyCredentialsProvider proxyCredentialsProvider,
                               String p12File, String passKey) {
        _proxyHost = proxyHost;
        _p12File = p12File;
        _passKey = passKey;
        _provider = proxyCredentialsProvider;
    }

    public HttpHost getHost() { return _proxyHost; }
    public String getP12File() { return _p12File; }
    public String getPassKey() { return _passKey; }
    public ProxyCredentialsProvider getProxyCredentialsProvider() { return _provider; }

    public static ProxyConfiguration.Builder builder() {
        return new ProxyConfiguration.Builder();
    }

    public static class Builder {
        private ProxyCredentialsProvider _provider;
        private HttpHost _proxyHost;
        private String _p12File;
        private String _passKey;

        public ProxyConfiguration.Builder credentialsProvider(ProxyCredentialsProvider provider) {
            _provider = provider;
            return this;
        }

        public ProxyConfiguration.Builder url(URL url) throws MalformedURLException {
            try {
                _proxyHost = new HttpHost(url.getProtocol(), url.getHost(), url.getPort());
            } catch (Exception exc) {
                throw new MalformedURLException("Proxy configuration is ignored. The proxy `url` was not provided or is malformed");
            }
            return this;
        }

        public ProxyConfiguration.Builder mtls(String p12File, String passKey) {
            _passKey = passKey;
            _p12File = p12File;
            return this;
        }

        public ProxyConfiguration build() {
            return new ProxyConfiguration(_proxyHost, _provider, _p12File, _passKey);
        }
    }
}