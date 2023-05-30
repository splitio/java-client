package io.split.engine.sse.workers;

import io.split.client.utils.Json;
import io.split.engine.common.Synchronizer;
import io.split.engine.common.SynchronizerImp;
import io.split.engine.experiments.SplitParser;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.RawMessageNotification;
import io.split.storages.SplitCacheProducer;
import org.junit.Test;
import org.mockito.Mockito;

public class FeatureFlagWorkerImpTest {

    @Test
    public void testRefreshSplitsWithCorrectFF(){
        SplitParser splitParser = new SplitParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, splitCacheProducer);
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":2,\\\"d\\\":\\\"eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
        FeatureFlagChangeNotification featureFlagChangeNotification = new FeatureFlagChangeNotification(genericNotificationData);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        Mockito.verify(splitCacheProducer, Mockito.times(1)).updateFeatureFlag(Mockito.anyObject());
        Mockito.verify(synchronizer, Mockito.times(0)).refreshSplits(1684265694505L);
    }

    @Test
    public void testRefreshSplitsWithEmptyData(){
        SplitParser splitParser = new SplitParser();
        Synchronizer synchronizer = Mockito.mock(SynchronizerImp.class);
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        FeatureFlagWorkerImp featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, splitCacheProducer);
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
        FeatureFlagChangeNotification featureFlagChangeNotification = new FeatureFlagChangeNotification(genericNotificationData);
        featureFlagsWorker.executeRefresh(featureFlagChangeNotification);
        Mockito.verify(splitCacheProducer, Mockito.times(0)).updateFeatureFlag(Mockito.anyObject());
        Mockito.verify(synchronizer, Mockito.times(1)).refreshSplits(1684265694505L);
    }

}