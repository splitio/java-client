package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;
import io.split.client.interceptors.SdkMetadataInterceptorFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class Metadata {

    /* package private */ static final String FIELD_HOSTNAME = "n";
    /* package private */ static final String FIELD_IP = "i";
    /* package private */ static final String FIELD_SDK_VERSION = "s";

    private static final Logger _log = LoggerFactory.getLogger(Metadata.class);

    @SerializedName(FIELD_HOSTNAME)
    private final String _hostname;
    @SerializedName(FIELD_IP)
    private final String _ip;
    @SerializedName(FIELD_SDK_VERSION)
    private final String _sdkVersion;

    public Metadata(boolean ipAddressEnabled, String sdkVersion) {
        String hostName = null;
        String ip = null;

        if (ipAddressEnabled) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                hostName = localHost.getHostName();
                ip = localHost.getHostAddress();
            } catch (Exception e) {
                _log.error("Could not resolve InetAddress", e);
            }
        }
        _sdkVersion = sdkVersion;
        _hostname = hostName;
        _ip = ip;
    }
}
