package io.split.telemetry.domain.enums;

public enum HttpParamsWrapper {
    EVENTS(HTTPLatenciesEnum.EVENTS, LastSynchronizationRecordsEnum.EVENTS, ResourceEnum.EVENT_SYNC),
    TELEMETRY(HTTPLatenciesEnum.TELEMETRY, LastSynchronizationRecordsEnum.TELEMETRY, ResourceEnum.TELEMETRY_SYNC),
    IMPRESSIONS(HTTPLatenciesEnum.IMPRESSIONS, LastSynchronizationRecordsEnum.IMPRESSIONS, ResourceEnum.IMPRESSION_SYNC),
    IMPRESSIONS_COUNT(HTTPLatenciesEnum.IMPRESSIONS_COUNT, LastSynchronizationRecordsEnum.IMPRESSIONS_COUNT, ResourceEnum.IMPRESSION_COUNT_SYNC),
    SEGMENTS(HTTPLatenciesEnum.SEGMENTS, LastSynchronizationRecordsEnum.SEGMENTS, ResourceEnum.SEGMENT_SYNC),
    SPLITS(HTTPLatenciesEnum.SPLITS, LastSynchronizationRecordsEnum.SPLITS, ResourceEnum.SPLIT_SYNC),
    TOKEN(HTTPLatenciesEnum.TOKEN, LastSynchronizationRecordsEnum.TOKEN, ResourceEnum.TOKEN_SYNC);


    private HTTPLatenciesEnum _httpLatenciesEnum;
    private LastSynchronizationRecordsEnum _lastSynchronizationRecordsEnum;
    private ResourceEnum _resourceEnum;

    HttpParamsWrapper(HTTPLatenciesEnum httpLatenciesEnum, LastSynchronizationRecordsEnum lastSynchronizationRecordsEnum, ResourceEnum resourceEnum) {
        _httpLatenciesEnum = httpLatenciesEnum;
        _lastSynchronizationRecordsEnum = lastSynchronizationRecordsEnum;
        _resourceEnum = resourceEnum;
    }

    public HTTPLatenciesEnum getHttpLatenciesEnum() {
        return _httpLatenciesEnum;
    }

    public LastSynchronizationRecordsEnum getLastSynchronizationRecordsEnum() {
        return _lastSynchronizationRecordsEnum;
    }

    public ResourceEnum getResourceEnum() {
        return _resourceEnum;
    }
}
