package io.split.kerberos;

import java.io.IOException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.logging.HttpLoggingInterceptor;

public class SplitHttpClientKerberosBuilder {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;

    public static OkHttpClient buildOkHttpClient(Proxy proxy, String proxyKerberosPrincipalName,
                                                 boolean debugEnabled, int readTimeout, int connectionTimeout) throws IOException {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (debugEnabled) {
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        if (connectionTimeout <= 0 || connectionTimeout > DEFAULT_CONNECTION_TIMEOUT) {
            connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        }
        if (readTimeout <= 0 || readTimeout > DEFAULT_READ_TIMEOUT) {
            readTimeout = DEFAULT_READ_TIMEOUT;
        }

        Map<String, String> kerberosOptions = new HashMap<>();
        kerberosOptions.put("com.sun.security.auth.module.Krb5LoginModule", "required");
        kerberosOptions.put("refreshKrb5Config", "false");
        kerberosOptions.put("doNotPrompt", "false");
        kerberosOptions.put("useTicketCache", "true");

        Authenticator proxyAuthenticator = getProxyAuthenticator(proxyKerberosPrincipalName, kerberosOptions);

        return new Builder()
                .proxy(proxy)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                .addInterceptor(logging)
                .proxyAuthenticator(proxyAuthenticator)
                .build();
    }

    public static HTTPKerberosAuthInterceptor getProxyAuthenticator(String proxyKerberosPrincipalName,
                                                                       Map<String, String> kerberosOptions) throws IOException {
        return new HTTPKerberosAuthInterceptor(proxyKerberosPrincipalName, kerberosOptions);
    }
}
