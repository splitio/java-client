package pluggable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class NotPipelinedImpl implements Pipeline {

    private final List<Callable<Object>> _methods;
    private final CustomStorageWrapper _storage;

    public NotPipelinedImpl(CustomStorageWrapper storage) {
        _methods = new ArrayList<>();
        _storage = storage;
    }

    @Override
    public List<Result> exec() throws Exception {
        List<Result> result = new ArrayList<>();
        for (Callable<Object> method : _methods) {
            result.add(new Result(method.call()));
        }
        return result;
    }

    @Override
    public void hIncrement(String key, String field, long value) {
        _methods.add(() -> { return _storage.hIncrement(key, field, value);});
    }

    @Override
    public void getMembers(String key) throws Exception {
        _methods.add(() -> { return _storage.getMembers(key);});
    }
}