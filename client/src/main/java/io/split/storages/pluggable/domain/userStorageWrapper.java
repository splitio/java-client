package io.split.storages.pluggable.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;
import pluggable.HasPipelineSupport;
import pluggable.NotPipelinedImpl;
import pluggable.Pipeline;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class userStorageWrapper implements CustomStorageWrapper {

    private static final Logger _log = LoggerFactory.getLogger(userStorageWrapper.class);

    private final CustomStorageWrapper _customStorageWrapper;

    public userStorageWrapper(CustomStorageWrapper customStorageWrapper) {
        _customStorageWrapper = checkNotNull(customStorageWrapper);
    }

    @Override
    public String get(String key) {
        try {
            return _customStorageWrapper.get(key);
        }
        catch (Exception e) {
            _log.error(String.format("error fetching key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return null;
        }
    }

    @Override
    public List<String> getMany(List<String> keys) {
        try {
            return _customStorageWrapper.getMany(keys);
        }
        catch (Exception e) {
            _log.error(String.format("error fetching keys '%s' from storage. Error: '%s'", keys, e.getMessage()));
            return null;
        }
    }

    @Override
    public void set(String key, String item) {
        try {
            _customStorageWrapper.set(key, item);
        }
        catch (Exception e) {
            _log.error(String.format("error updating key '%s' from storage. Error: '%s'", key, e.getMessage()));
        }
    }

    @Override
    public void delete(List<String> keys) {
        try {
            _customStorageWrapper.delete(keys);
        }
        catch (Exception e) {
            _log.error(String.format("error deleting keys '%s' from storage. Error: '%s'", keys, e.getMessage()));
        }
    }

    @Override
    public String getAndSet(String key, String item){
        try {
            return _customStorageWrapper.getAndSet(key, item);
        }
        catch (Exception e) {
            _log.error(String.format("error getting and updating key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return null;
        }
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix){
        try {
            return _customStorageWrapper.getKeysByPrefix(prefix);
        }
        catch (Exception e) {
            _log.error(String.format("error getting keys '%s' from storage. Error: '%s'", prefix, e.getMessage()));
            return null;
        }
    }

    @Override
    public long increment(String key, long value) {
        try {
            return _customStorageWrapper.increment(key, value);
        }
        catch (Exception e) {
            _log.error(String.format("error incrementing key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return 0L;
        }
    }

    @Override
    public long hIncrement(String key, String field, long value){
        try {
            return _customStorageWrapper.hIncrement(key, field, value);
        }
        catch (Exception e) {
            _log.error(String.format("error incrementing key by field '%s' from storage. Error: '%s'", key, e.getMessage()));
            return 0L;
        }
    }

    @Override
    public long decrement(String key, long value) {
        try {
            return _customStorageWrapper.decrement(key, value);
        }
        catch (Exception e) {
            _log.error(String.format("error decrementing key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return 0L;
        }
    }

    @Override
    public long pushItems(String key, List<String> items) {
        try {
            return _customStorageWrapper.pushItems(key, items);
        }
        catch (Exception e) {
            _log.error(String.format("error pushing items with key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return 0;
        }
    }

    @Override
    public List<String> popItems(String key, long count){
        try {
            return _customStorageWrapper.popItems(key, count);
        }
        catch (Exception e) {
            _log.error(String.format("error popping key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return null;
        }
    }

    @Override
    public long getItemsCount(String key) {
        try {
            return _customStorageWrapper.getItemsCount(key);
        }
        catch (Exception e) {
            _log.error(String.format("error getting items count key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return -1L;
        }
    }

    @Override
    public boolean itemContains(String key, String item){
        try {
            return _customStorageWrapper.itemContains(key, item);
        }
        catch (Exception e) {
            _log.error(String.format("error checking if item contains key '%s' from storage. Error: '%s'", key, e.getMessage()));
            return false;
        }
    }

    @Override
    public void addItems(String key, List<String> items) {
        try {
            _customStorageWrapper.addItems(key, items);
        }
        catch (Exception e) {
            _log.error(String.format("error adding items with key '%s' from storage. Error: '%s'", key, e.getMessage()));
        }
    }

    @Override
    public void removeItems(String key, List<String> items) {
        try {
            _customStorageWrapper.removeItems(key, items);
        }
        catch (Exception e) {
            _log.error(String.format("error removing items with key '%s' from storage. Error: '%s'", key, e.getMessage()));
        }
    }

    @Override
    public List<String> getItems(List<String> keys){
        try {
            return _customStorageWrapper.getItems(keys);
        }
        catch (Exception e) {
            _log.error(String.format("error getting items with keys '%s' from storage. Error: '%s'", keys, e.getMessage()));
            return null;
        }
    }

    @Override
    public boolean connect(){
        try {
            return _customStorageWrapper.connect();
        }
        catch (Exception e) {
            _log.error(String.format("error trying to connect. Error: '%s'" , e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean disconnect(){
        try {
            return _customStorageWrapper.disconnect();
        }
        catch (Exception e) {
            _log.error(String.format("error trying to disconnect. Error: '%s'" , e.getMessage()));
            return false;
        }
    }

    public Pipeline pipeline() throws Exception {
        return (_customStorageWrapper instanceof HasPipelineSupport)
                ? ((HasPipelineSupport) _customStorageWrapper).pipeline()
                : new NotPipelinedImpl(_customStorageWrapper);
    }
}
