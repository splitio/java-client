package redis.common;

public class CommonRedis {

    public static final String TELEMETRY_INIT = "SPLITIO.telemetry.init" ;
    public static final String EVENTS_KEY = "SPLITIO.events" ;
    public static final String IMPRESSIONS_KEY = "SPLITIO.impressions" ;
    public static final long IMPRESSIONS_OR_EVENTS_DEFAULT_TTL = 3600000L;

    private final String _prefix;

    private CommonRedis (String prefix){
        _prefix = prefix;
    }
    public static CommonRedis create(String prefix) {
        return new CommonRedis(prefix);
    }

    public String buildKeyWithPrefix(String key) {
        if (_prefix.isEmpty()) {
            return key;
        }
        return String.format("%s.%s", _prefix, key);
    }

    public String getPrefix() {
        return _prefix;
    }
}