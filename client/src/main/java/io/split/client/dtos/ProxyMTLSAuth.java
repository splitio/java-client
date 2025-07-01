package io.split.client.dtos;

public class ProxyMTLSAuth {
    private final String _proxyP12File;
    private final String _proxyP12FilePassKey;

    private ProxyMTLSAuth(String proxyP12File, String proxyP12FilePassKey) {
        _proxyP12File = proxyP12File;
        _proxyP12FilePassKey = proxyP12FilePassKey;
    }

    public String getP12File() { return _proxyP12File; }

    public String getP12FilePassKey() { return _proxyP12FilePassKey; }

    public static ProxyMTLSAuth.Builder builder() {
        return new ProxyMTLSAuth.Builder();
    }

    public static class Builder {
        private String _p12File;
        private String _p12FilePassKey;
        
        public ProxyMTLSAuth.Builder proxyP12File(String p12File) {
            _p12File = p12File;
            return this;
        }

        public ProxyMTLSAuth.Builder proxyP12FilePassKey(String p12FilePassKey) {
            _p12FilePassKey = p12FilePassKey;
            return this;
        }

        public ProxyMTLSAuth build() {
            return new ProxyMTLSAuth(_p12File, _p12FilePassKey);
        }
    }
}