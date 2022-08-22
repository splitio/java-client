package io.split.storages.pluggable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.split.client.dtos.Condition;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Split;
import io.split.client.dtos.Status;
import io.split.client.utils.Json;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.segments.SegmentImp;
import io.split.grammar.Treatments;
import io.split.storages.pluggable.domain.ConfigConsumer;
import io.split.storages.pluggable.domain.EventConsumer;
import io.split.storages.pluggable.domain.ImpressionConsumer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.utils.AtomicLongArray;
import pluggable.CustomStorageWrapper;
import pluggable.NotPipelinedImpl;
import pluggable.Pipeline;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class CustomStorageWrapperImp implements CustomStorageWrapper {

    public static final int MAX_LATENCY_BUCKET_COUNT = 23;
    private static final String TELEMETRY = "SPLITIO.telemetry";
    private static final String LATENCIES = "SPLITIO.telemetry.latencies";
    private static final String SPLIT = "SPLITIO.split.";
    private static final String SPLITS = "SPLITIO.splits.*";
    private static final String SEGMENT = "SPLITIO.segment.";
    private static final String IMPRESSIONS = "SPLITIO.impressions";
    private static final String EVENTS = "SPLITIO.events";
    private static final String COUNTS = "SPLITIO.impressions.counts";
    private Map<String, Split> splitsStorage = new HashMap<>();
    private Map<String, SegmentImp> segmentStorage = new HashMap<>();
    private final ConcurrentMap<String, AtomicLongArray> _methodLatencies = Maps.newConcurrentMap();
    private final ConcurrentMap<String, Long> _latencies = Maps.newConcurrentMap();
    private final ConcurrentMap<String, Long> _impressionsCount = Maps.newConcurrentMap();
    private ConfigConsumer _telemetryInit = null;
    private List<ImpressionConsumer> imps = new ArrayList<>();
    private List<EventConsumer> events = new ArrayList<>();
    private final Gson _json = new GsonBuilder()
            .serializeNulls()  // Send nulls
            .excludeFieldsWithModifiers(Modifier.STATIC)
            .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            })
            .create();

    public CustomStorageWrapperImp() {
        _methodLatencies.put(MethodEnum.TREATMENT.getMethod(), new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENTS.getMethod(), new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENT_WITH_CONFIG.getMethod(), new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TREATMENTS_WITH_CONFIG.getMethod(), new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        _methodLatencies.put(MethodEnum.TRACK.getMethod(), new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        updateCache();
    }

    @Override
    public String get(String key) throws Exception {
        String value = getStorage(key);
        if(value.equals(SPLIT)){
            return _json.toJson(splitsStorage.get(key));
        }
        return "";
    }

    @Override
    public List<String> getMany(List<String> keys) throws Exception {
        if(keys == null || keys.size()==0){
            return null;
        }
        String value = getStorage(keys.get(0));
        if(value.equals(SPLIT)){
            List<String> results = new ArrayList<>();
            keys.forEach(k -> results.add(Json.toJson(splitsStorage.get(k))));
            return results;
        }
        return null;
    }

    @Override
    public void set(String key, String item) throws Exception {
        String value = getStorage(key);
        if(value.equals(TELEMETRY)) {
            if (key.contains("init")) {
                _telemetryInit = _json.fromJson(item, ConfigConsumer.class);
            }
        }
    }

    @Override
    public void delete(List<String> keys) throws Exception {

    }

    @Override
    public String getAndSet(String key, String item) throws Exception {
        return null;
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) throws Exception {
        String value = getStorage(prefix);
        if(value.equals(SPLIT)){
            return splitsStorage.keySet();
        }
        return null;
    }

    @Override
    public long increment(String key, long value) throws Exception {
        return 0;
    }

    @Override
    public long decrement(String key, long value) throws Exception {
        return 0;
    }

    @Override
    public long hIncrement(String key, String field, long value) throws Exception {
        String storageKey = getStorage(key);
        Long count = 0L;
        if (storageKey.equals(COUNTS)){
            if(_impressionsCount.containsKey(field)){
                count = _impressionsCount.get(field);
            }
            count += value;
            _impressionsCount.put(field, count);
            return count;
        }
        if(storageKey.equals(LATENCIES)){
            if(_latencies.containsKey(field)){
                count = _latencies.get(field);
            }
            count += value;
            _latencies.put(field, count);
        }
        return count;
    }

    @Override
    public long pushItems(String key, List<String> items) throws Exception {
        String value = getStorage(key);
        if(value.equals(IMPRESSIONS)){
            items.forEach(imp -> imps.add(_json.fromJson(imp, ImpressionConsumer.class)));
        }
        else if(value.equals(EVENTS)) {
            items.forEach(ev -> events.add(_json.fromJson(ev, EventConsumer.class)));
        }
        return 0;
    }

    @Override
    public List<String> popItems(String key, long count) throws Exception {
        return null;
    }

    @Override
    public long getItemsCount(String key) throws Exception {
        return 0;
    }

    @Override
    public boolean itemContains(String key, String item) throws Exception {
        String value = getStorage(key);
        if(value.equals(SEGMENT)){
            SegmentImp segmentImp = segmentStorage.get(key);
            return segmentImp.contains(item);
        }
        return false;
    }

    @Override
    public void addItems(String key, List<String> items) throws Exception {

    }

    @Override
    public void removeItems(String key, List<String> items) throws Exception {

    }

    @Override
    public List<String> getItems(List<String> keys) throws Exception {
        return null;
    }

    @Override
    public boolean connect() throws Exception {
        return true;
    }

    @Override
    public boolean disconnect() throws Exception {
        return false;
    }

    @Override
    public Pipeline pipeline() throws Exception {
        return new NotPipelinedImpl(this);
    }

    private String getStorage(String key) {
        if(key.startsWith(SPLITS))
            return SPLITS;
        else if(key.startsWith(SPLIT))
            return SPLIT;
        else if (key.startsWith(LATENCIES))
            return LATENCIES;
        else if(key.startsWith(TELEMETRY))
            return TELEMETRY;
        else if(key.startsWith(SEGMENT))
            return SEGMENT;
        else if(key.startsWith(COUNTS))
            return  COUNTS;
        else if(key.startsWith(IMPRESSIONS))
            return IMPRESSIONS;
        else if(key.startsWith(EVENTS))
            return EVENTS;
        return "";
    }

    private void updateCache(){
        Condition condition = ConditionsTestUtil.makeUserDefinedSegmentCondition(ConditionType.WHITELIST,"segmentName" , Lists.newArrayList(ConditionsTestUtil.partition("on", 100)));
        segmentStorage.put(PrefixAdapter.buildSegment("segmentName"), new SegmentImp(9874654L, "segmentName", Lists.newArrayList("key", "key2")));
        splitsStorage.put(PrefixAdapter.buildSplitKey("first.name"), makeSplit("first.name", 123, Lists.newArrayList(condition), 456478976L));
        splitsStorage.put(PrefixAdapter.buildSplitKey("second.name"), makeSplit("second.name", 321, Lists.newArrayList(), 568613L));
    }

    private Split makeSplit(String name, int seed, List<Condition> conditions, long changeNumber) {
        Split split = new Split();
        split.name = name;
        split.seed = seed;
        split.trafficAllocation = 100;
        split.trafficAllocationSeed = seed;
        split.status = Status.ACTIVE;
        split.conditions = conditions;
        split.defaultTreatment = Treatments.OFF;
        split.trafficTypeName = "user";
        split.changeNumber = changeNumber;
        split.algo = 1;
        split.configurations = null;
        return split;
    }

    public ConcurrentMap<String, Long> getLatencies() {
        return _latencies;
    }

    public ConcurrentMap<String, Long> get_impressionsCount() {
        return _impressionsCount;
    }

    public List<ImpressionConsumer> getImps() {
        return imps;
    }

    public List<EventConsumer> getEvents() {
        return events;
    }

    public ConfigConsumer get_telemetryInit() {
        return _telemetryInit;
    }
}