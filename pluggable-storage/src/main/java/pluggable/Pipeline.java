package pluggable;

import java.util.List;

public interface Pipeline {
    List<Result> exec() throws Exception;

    void increment(String key, long value) throws Exception;
}
