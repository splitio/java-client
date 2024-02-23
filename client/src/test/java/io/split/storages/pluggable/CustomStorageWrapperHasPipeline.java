package io.split.storages.pluggable;

import com.google.common.collect.Maps;
import pluggable.CustomStorageWrapper;
import pluggable.HasPipelineSupport;
import pluggable.Pipeline;
import pluggable.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

public class CustomStorageWrapperHasPipeline implements CustomStorageWrapper, HasPipelineSupport {

    private static final String COUNTS = "SPLITIO.impressions.count";
    private static final String FLAG_SET = "SPLITIO.flagSet";
    private final ConcurrentMap<String, Long> _impressionsCount = Maps.newConcurrentMap();
    private final ConcurrentMap<String, HashSet<String>> _flagSets = Maps.newConcurrentMap();

    public CustomStorageWrapperHasPipeline() {
        _flagSets.put("SPLITIO.flagSet.set1", new HashSet<>(new ArrayList<>(Arrays.asList("flag1", "flag2"))));
    }

    @Override
    public String get(String key) throws Exception {
        return null;
    }

    @Override
    public List<String> getMany(List<String> keys) {
        return null;
    }

    @Override
    public void set(String key, String item) throws Exception {

    }

    @Override
    public void hSet(String key, String field, String item) {

    }

    @Override
    public void delete(List<String> keys) {

    }

    @Override
    public String getAndSet(String key, String item) {
        return null;
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) {
        return null;
    }

    @Override
    public long increment(String key, long value) {
        return 0;
    }

    @Override
    public long decrement(String key, long value) {
        return 0;
    }

    @Override
    public long hIncrement(String key, String field, long value) {
        return 0;
    }

    @Override
    public long pushItems(String key, List<String> items) {
        return 0;
    }

    @Override
    public List<String> popItems(String key, long count) {
        return null;
    }

    @Override
    public long getItemsCount(String key) {
        return 0;
    }

    @Override
    public boolean itemContains(String key, String item) {
        return false;
    }

    @Override
    public void addItems(String key, List<String> items) {

    }

    @Override
    public void removeItems(String key, List<String> items) {

    }

    @Override
    public List<String> getItems(List<String> keys) {
        return null;
    }

    @Override
    public Set<String> getMembers(String key) {
        return null;
    }

    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public boolean disconnect() {
        return false;
    }

    @Override
    public Pipeline pipeline() {
        return new CustomPipeline();
    }

    public ConcurrentMap<String, Long> getImpressionsCount(){
        return _impressionsCount;
    }
    private class CustomPipeline implements Pipeline {

        private List<Callable<Object>> methodsToExecute;

        public CustomPipeline() {
            this.methodsToExecute = new ArrayList<>();
        }

        @Override
        public List<Result> exec() throws Exception {
            List<Result> result = new ArrayList<>();
            for (Callable<Object> method : methodsToExecute) {
                result.add(new Result(method.call()));
            }
            return result;
        }

        @Override
        public void hIncrement(String key, String field, long value) {
            methodsToExecute.add(() -> { return  hIncrementToExecute(key, field, value);});
        }

        public long hIncrementToExecute(String key, String field, long value){
            String storageKey = getStorage(key);
            Long count = 0L;
            if (storageKey.equals(COUNTS)){
                if(_impressionsCount.containsKey(field)){
                    count = _impressionsCount.get(field);
                }
                count += value;
                _impressionsCount.put(field, count);
            }
            return count;
        }

        @Override
        public void getMembers(String key) {
            methodsToExecute.add(() -> { return  getMembersToExecute(key);});
        }

        private HashSet<String> getMembersToExecute(String key) {
            String storageKey = getStorage(key);
            if(storageKey.equals(FLAG_SET)) {
                return _flagSets.get(key);
            }
            return new HashSet<>();
        }

        private String getStorage(String key) {
            if(key.startsWith(COUNTS))
                return  COUNTS;
            if(key.startsWith(FLAG_SET))
                return FLAG_SET;
            return "";
        }

        public ConcurrentMap<String, Long> getImpressionsCount(){
            return _impressionsCount;
        }
    }
}