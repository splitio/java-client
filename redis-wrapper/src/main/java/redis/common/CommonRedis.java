package redis.common;

public class CommonRedis {

    private final String _prefix;

    private CommonRedis (String prefix){
        _prefix = prefix;
    }
    public static CommonRedis create(String prefix) {
        return new CommonRedis(prefix);
    }

    public String buildKeyWithPrefix(String key) {
        return String.format("%s.%s", _prefix, key);
    }

}