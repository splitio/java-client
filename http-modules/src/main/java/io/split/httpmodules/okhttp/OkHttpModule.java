package io.split.httpmodules.okhttp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import io.split.client.RequestDecorator;
import io.split.client.utils.SDKMetadata;
import io.split.service.CustomHttpModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkHttpModule implements CustomHttpModule {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;
    private final boolean _debugEnabled;
    private final int _connectionTimeout;
    private final int _readTimeout;
    private final Proxy _proxy;
    private final ProxyAuthScheme _proxyAuthScheme;
    private final String _proxyAuthKerberosPrincipalName;
    private static final Logger _log = LoggerFactory.getLogger(OkHttpModule.class);

    public static Builder builder() {
        return new Builder();
    }

    private OkHttpModule(ProxyAuthScheme proxyAuthScheme,
                         String proxyAuthKerberosPrincipalName,
                         Proxy proxy,
                        int connectionTimeout,
                        int readTimeout,
                         boolean debugEnabled) {
        _proxyAuthScheme = proxyAuthScheme;
        _proxyAuthKerberosPrincipalName = proxyAuthKerberosPrincipalName;
        _proxy = proxy;
        _connectionTimeout = connectionTimeout;
        _readTimeout = readTimeout;
        _debugEnabled = debugEnabled;
    }

    @Override
    public OkHttpClientImpl createClient(String apiToken, SDKMetadata sdkMetadata, RequestDecorator requestDecorator) throws IOException {
        return new OkHttpClientImpl(apiToken, sdkMetadata, requestDecorator,
                _proxy, _proxyAuthKerberosPrincipalName, _debugEnabled,
                _readTimeout, _connectionTimeout);
    }

    public Proxy proxy() {
        return _proxy;
    }
    public ProxyAuthScheme proxyAuthScheme() {
        return _proxyAuthScheme;
    }
    public String proxyKerberosPrincipalName() { return _proxyAuthKerberosPrincipalName; }
    public int connectionTimeout() {
        return _connectionTimeout;
    }
    public boolean debugEnabled() {
        return _debugEnabled;
    }
    public int readTimeout() {
        return _readTimeout;
    }

    public static final class Builder {
        private int _connectionTimeout = 15000;
        private int _readTimeout = 15000;
        private String _proxyHost = "localhost";
        private int _proxyPort = -1;
        private ProxyAuthScheme _proxyAuthScheme = null;
        private String _proxyAuthKerberosPrincipalName = null;
        private boolean _debugEnabled = false;

        public Builder() {
        }

        public Builder debugEnabled() {
            _debugEnabled = true;
            return this;
        }

        /**
         * The host location of the proxy. Default is localhost.
         *
         * @param proxyHost location of the proxy
         * @return this builder
         */
        public Builder proxyHost(String proxyHost) {
            _proxyHost = proxyHost;
            return this;
        }

        /**
         * The port of the proxy. Default is -1.
         *
         * @param proxyPort port for the proxy
         * @return this builder
         */
        public Builder proxyPort(int proxyPort) {
            _proxyPort = proxyPort;
            return this;
        }

        Proxy proxy() {
            if (_proxyPort != -1) {
                return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(_proxyHost, _proxyPort));
            }
            // Default is no proxy.
            return null;
        }

        /**
         * Authentication Scheme
         *
         * @param proxyAuthScheme
         * @return this builder
         */
        public Builder proxyAuthScheme(ProxyAuthScheme proxyAuthScheme) {
            _proxyAuthScheme = proxyAuthScheme;
            return this;
        }

        /**
         * Kerberos Principal Account Name
         *
         * @param proxyAuthKerberosPrincipalName
         * @return this builder
         */
        public Builder proxyAuthKerberosPrincipalName(String proxyAuthKerberosPrincipalName) {
            _proxyAuthKerberosPrincipalName = proxyAuthKerberosPrincipalName;
            return this;
        }

        private void verifyAuthScheme() {
            if (_proxyAuthScheme == ProxyAuthScheme.KERBEROS) {
                if (proxy() == null) {
                    throw new IllegalStateException("Kerberos mode require Proxy parameters.");
                }
                if (_proxyAuthKerberosPrincipalName == null) {
                    throw new IllegalStateException("Kerberos mode require Kerberos Principal Name.");
                }
            }
        }

        private void verifyTimeouts() {
            if (_connectionTimeout <= 0 || _connectionTimeout > DEFAULT_CONNECTION_TIMEOUT) {
                _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
            }
            if (_readTimeout <= 0 || _readTimeout > DEFAULT_READ_TIMEOUT) {
                _readTimeout = DEFAULT_READ_TIMEOUT;
            }
        }

        public OkHttpModule build()  {
            verifyTimeouts();
            verifyAuthScheme();

            return new OkHttpModule(
                    _proxyAuthScheme,
                    _proxyAuthKerberosPrincipalName,
                    proxy(),
                    _connectionTimeout,
                    _readTimeout,
                    _debugEnabled);
        }
    }
}
