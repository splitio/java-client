package io.split.telemetry.domain;

public class TaskPeriods {
    private int _splitSync;
    private int _segmentSync;
    private int _impressionSync;
    private int _gaugeSync;
    private int _counterSync;
    private int _latencySync;
    private int _eventsSync;

    public int get_splitSync() {
        return _splitSync;
    }

    public void set_splitSync(int _splitSync) {
        this._splitSync = _splitSync;
    }

    public int get_segmentSync() {
        return _segmentSync;
    }

    public void set_segmentSync(int _segmentSync) {
        this._segmentSync = _segmentSync;
    }

    public int get_impressionSync() {
        return _impressionSync;
    }

    public void set_impressionSync(int _impressionSync) {
        this._impressionSync = _impressionSync;
    }

    public int get_gaugeSync() {
        return _gaugeSync;
    }

    public void set_gaugeSync(int _gaugeSync) {
        this._gaugeSync = _gaugeSync;
    }

    public int get_counterSync() {
        return _counterSync;
    }

    public void set_counterSync(int _counterSync) {
        this._counterSync = _counterSync;
    }

    public int get_latencySync() {
        return _latencySync;
    }

    public void set_latencySync(int _latencySync) {
        this._latencySync = _latencySync;
    }

    public int get_eventsSync() {
        return _eventsSync;
    }

    public void set_eventsSync(int _eventsSync) {
        this._eventsSync = _eventsSync;
    }
}
