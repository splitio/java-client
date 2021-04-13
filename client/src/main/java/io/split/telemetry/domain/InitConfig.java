package io.split.telemetry.domain;

public class InitConfig {
    private AdvanceConfig _advanceConfig;
    private TaskPeriods _taskPeriods;
    private ManagerConfig _managerConfig;

    public AdvanceConfig get_advanceConfig() {
        return _advanceConfig;
    }

    public void set_advanceConfig(AdvanceConfig _advanceConfig) {
        this._advanceConfig = _advanceConfig;
    }

    public TaskPeriods get_taskPeriods() {
        return _taskPeriods;
    }

    public void set_taskPeriods(TaskPeriods _taskPeriods) {
        this._taskPeriods = _taskPeriods;
    }

    public ManagerConfig get_managerConfig() {
        return _managerConfig;
    }

    public void set_managerConfig(ManagerConfig _managerConfig) {
        this._managerConfig = _managerConfig;
    }
}
