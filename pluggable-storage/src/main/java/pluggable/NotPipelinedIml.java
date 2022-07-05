package pluggable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class NotPipelinedIml implements PipelineWrapper {

    private List<Callable<Object>> _methods;
    private CustomStorageWrapper _storage;

    public NotPipelinedIml(CustomStorageWrapper storage) {
        _methods = new ArrayList<>();
        _storage = storage;
    }

    @Override
    public List<Object> exec() throws Exception {
        List<Object> result = new ArrayList<>();
        for (Callable<Object> method : _methods) {
            result.add(method.call());
        }
        return result;
    }

    @Override
    public void increment(String key, long value) throws Exception {
        _methods.add(() -> { return _storage.increment(key, value);});
    }
}
