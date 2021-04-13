package io.split.telemetry.domain;

public class ManagerConfig {
    private String _operationMode;
    private String _impressionsMode;
    private boolean _listenerEnabled;

    public String get_operationMode() {
        return _operationMode;
    }

    public void set_operationMode(String _operationMode) {
        this._operationMode = _operationMode;
    }

    public String get_impressionsMode() {
        return _impressionsMode;
    }

    public void set_impressionsMode(String _impressionsMode) {
        this._impressionsMode = _impressionsMode;
    }

    public boolean is_listenerEnabled() {
        return _listenerEnabled;
    }

    public void set_listenerEnabled(boolean _listenerEnabled) {
        this._listenerEnabled = _listenerEnabled;
    }
}
