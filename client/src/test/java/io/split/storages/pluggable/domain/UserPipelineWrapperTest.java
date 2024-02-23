package io.split.storages.pluggable.domain;

import io.split.storages.pluggable.CustomStorageWrapperHasPipeline;
import io.split.storages.pluggable.CustomStorageWrapperImp;
import org.junit.Assert;
import org.junit.Test;
import pluggable.NotPipelinedImpl;
import pluggable.Result;

import java.util.List;
import java.util.Optional;

public class UserPipelineWrapperTest {
    private static final String KEY = "SPLITIO.impressions.counts";
    private static final String SET_KET = "SPLITIO.flagSet";
    private static final String HASH_COUNT_KEY = "countKey";

    @Test
    public void testHincrementWithPipeline() throws Exception {
        CustomStorageWrapperHasPipeline customStorageWrapper = new CustomStorageWrapperHasPipeline();
        UserPipelineWrapper userPipelineWrapper = new UserPipelineWrapper(customStorageWrapper.pipeline());
        userPipelineWrapper.hIncrement(KEY, HASH_COUNT_KEY, 1);
        List<Result> results = userPipelineWrapper.exec();
        Assert.assertEquals(Optional.of(1L), results.get(0).asLong());
    }

    @Test
    public void testHincrementWithoutPipeline() throws Exception {
        CustomStorageWrapperImp customStorageWrapper = new CustomStorageWrapperImp();
        NotPipelinedImpl notPipelined = new NotPipelinedImpl(customStorageWrapper);
        UserPipelineWrapper userPipelineWrapper = new UserPipelineWrapper(notPipelined);
        userPipelineWrapper.hIncrement(KEY, HASH_COUNT_KEY, 1);
        List<Result> results = userPipelineWrapper.exec();
        Assert.assertEquals(Optional.of(1L), results.get(0).asLong());
    }

    @Test
    public void testGetMembersWithPipeline() throws Exception {
        CustomStorageWrapperHasPipeline customStorageWrapper = new CustomStorageWrapperHasPipeline();
        UserPipelineWrapper userPipelineWrapper = new UserPipelineWrapper(customStorageWrapper.pipeline());
        userPipelineWrapper.getMembers(SET_KET + ".set1");
        List<Result> results = userPipelineWrapper.exec();
        Assert.assertEquals(2, results.get(0).asHash().get().size());
    }

    @Test
    public void testGetMembersWithoutPipeline() throws Exception {
        CustomStorageWrapperImp customStorageWrapper = new CustomStorageWrapperImp();
        NotPipelinedImpl notPipelined = new NotPipelinedImpl(customStorageWrapper);
        notPipelined.getMembers(SET_KET + ".set1");
        List<Result> results = notPipelined.exec();
        Assert.assertEquals(2, results.get(0).asHash().get().size());
    }
}