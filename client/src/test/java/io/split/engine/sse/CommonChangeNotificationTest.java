package io.split.engine.sse;

import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.Split;
import io.split.client.dtos.Status;
import io.split.client.utils.Json;
import io.split.engine.sse.dtos.CommonChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.RawMessageNotification;
import io.split.engine.sse.enums.CompressType;
import org.junit.Assert;
import org.junit.Test;

public class CommonChangeNotificationTest {

    @Test
    public void testFeatureFlagNotification() {
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":2,\\\"d\\\":\\\"eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);

        CommonChangeNotification<Split> featureFlagChangeNotification = new CommonChangeNotification(genericNotificationData, Split.class);
        Assert.assertEquals(IncomingNotification.Type.SPLIT_UPDATE, featureFlagChangeNotification.getType());
        Assert.assertEquals(1684265694505L, featureFlagChangeNotification.getChangeNumber());
        Assert.assertEquals(CompressType.ZLIB, featureFlagChangeNotification.getCompressType());
        Assert.assertEquals(0L, featureFlagChangeNotification.getPreviousChangeNumber());
        Assert.assertEquals("mauro_java", featureFlagChangeNotification.getDefinition().name);
        Assert.assertEquals(-1769377604, featureFlagChangeNotification.getDefinition().seed);
    }

    @Test
    public void testRuleBasedSegmentNotification() {
        String notification = "{\"id\":\"vQQ61wzBRO:0:0\",\"clientId\":\"pri:MTUxNzg3MDg1OQ==\",\"timestamp\":1684265694676,\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MjkyNTIzNjczMw==_splits\",\"data\":\"{\\\"type\\\":\\\"RB_SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1684265694505,\\\"pcn\\\":0,\\\"c\\\":0,\\\"d\\\":\\\"eyJjaGFuZ2VOdW1iZXIiOiA1LCAibmFtZSI6ICJzYW1wbGVfcnVsZV9iYXNlZF9zZWdtZW50IiwgInN0YXR1cyI6ICJBQ1RJVkUiLCAidHJhZmZpY1R5cGVOYW1lIjogInVzZXIiLCAiZXhjbHVkZWQiOiB7ImtleXMiOiBbIm1hdXJvQHNwbGl0LmlvIiwgImdhc3RvbkBzcGxpdC5pbyJdLCAic2VnbWVudHMiOiBbXX0sICJjb25kaXRpb25zIjogW3sibWF0Y2hlckdyb3VwIjogeyJjb21iaW5lciI6ICJBTkQiLCAibWF0Y2hlcnMiOiBbeyJrZXlTZWxlY3RvciI6IHsidHJhZmZpY1R5cGUiOiAidXNlciIsICJhdHRyaWJ1dGUiOiAiZW1haWwifSwgIm1hdGNoZXJUeXBlIjogIkVORFNfV0lUSCIsICJuZWdhdGUiOiBmYWxzZSwgIndoaXRlbGlzdE1hdGNoZXJEYXRhIjogeyJ3aGl0ZWxpc3QiOiBbIkBzcGxpdC5pbyJdfX1dfX1dfQ==\\\"}\"}";
        RawMessageNotification rawMessageNotification = Json.fromJson(notification, RawMessageNotification.class);
        GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);

        CommonChangeNotification<RuleBasedSegment> ruleBasedSegmentCommonChangeNotification = new CommonChangeNotification(genericNotificationData, RuleBasedSegment.class);
        Assert.assertEquals(IncomingNotification.Type.RB_SEGMENT_UPDATE, ruleBasedSegmentCommonChangeNotification.getType());
        Assert.assertEquals(1684265694505L, ruleBasedSegmentCommonChangeNotification.getChangeNumber());
        Assert.assertEquals(CompressType.NOT_COMPRESSED, ruleBasedSegmentCommonChangeNotification.getCompressType());
        Assert.assertEquals(0L, ruleBasedSegmentCommonChangeNotification.getPreviousChangeNumber());
        Assert.assertEquals("sample_rule_based_segment", ruleBasedSegmentCommonChangeNotification.getDefinition().name);
        Assert.assertEquals(Status.ACTIVE, ruleBasedSegmentCommonChangeNotification.getDefinition().status);
    }
}