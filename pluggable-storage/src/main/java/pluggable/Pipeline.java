package pluggable;

import java.util.List;

public interface Pipeline {
    List<Result> exec() throws Exception;
    void hIncrement(String key, String field, long value);
    void getMembers(String key) throws Exception;
}
