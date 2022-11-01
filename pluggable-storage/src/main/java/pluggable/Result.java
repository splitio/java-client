package pluggable;

import java.util.Optional;

public class Result {
    private final Object _item;

    public Result(Object item) {
        _item = item;
    }

    public Optional<String> asString() {
        if (_item instanceof String) {
            return Optional.ofNullable((String)_item);
        }
        return Optional.empty();
    }

    public Optional<Long> asLong() {
        if (_item instanceof Long) {
            return Optional.ofNullable((Long)_item);
        }
        return Optional.empty();
    }
}
