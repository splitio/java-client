package io.split.storages.pluggable.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.Pipeline;
import pluggable.Result;

import java.util.ArrayList;
import java.util.List;

public class UserPipelineWrapper implements Pipeline{

    private static final Logger _logger = LoggerFactory.getLogger(UserPipelineWrapper.class);

    private final Pipeline _pipeline;


    public UserPipelineWrapper(Pipeline pipeline) {
        _pipeline = pipeline;
    }

    @Override
    public List<Result> exec() {
        try{
            return _pipeline.exec();
        } catch (Exception e) {
            _logger.warn("Exception calling Pipeline exec", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void hIncrement(String key, String field, long value) {
        try {
            _pipeline.hIncrement(key, field, value);
        } catch (Exception e){
           _logger.warn("Exception calling Pipeline hIncrement", e);
        }
    }
}
