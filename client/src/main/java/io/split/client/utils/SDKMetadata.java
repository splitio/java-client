package io.split.client.utils;

public class SDKMetadata {
    private String _sdkVersion;
    private String _machineIp;
    private String _machineName;

    public SDKMetadata(String sdkVersion, String machineIp, String machineName) {
        this._sdkVersion = sdkVersion;
        this._machineIp = machineIp;
        this._machineName = machineName;
    }

    public String getSdkVersion() {
        return _sdkVersion;
    }

    public String getMachineIp() {
        return _machineIp;
    }

    public String getMachineName() {
        return _machineName;
    }
}
