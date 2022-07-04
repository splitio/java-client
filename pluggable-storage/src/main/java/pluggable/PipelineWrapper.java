package pluggable;

import java.util.List;

public interface PipelineWrapper {
    List<Object> exec() throws Exception;

    void increment(String key, long value) throws Exception;
}
