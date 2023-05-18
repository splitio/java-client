package io.split.engine.sse.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class DecompressionUtilTest {


    @Test
    public void testZLibDecompress() throws UnsupportedEncodingException {
        String toDecode = "eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=";

        byte[] decodedBytes = Base64.getDecoder().decode(toDecode);
        byte[] decompressFeatureFlag = DecompressionUtil.zLibDecompress(decodedBytes);
        String featureFlag = new String(decompressFeatureFlag, 0, decompressFeatureFlag.length, "UTF-8");
        Assert.assertEquals("{\"trafficTypeName\":\"user\",\"id\":\"d431cdd0-b0be-11ea-8a80-1660ada9ce39\",\"name\":\"mauro_java\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-92391491,\"seed\":-1769377604,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1684265694505,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"WHITELIST\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"matcherType\":\"WHITELIST\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"admin\",\"mauro\",\"nico\"]}}]},\"partitions\":[{\"treatment\":\"v5\",\"size\":100}],\"label\":\"whitelisted\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"maur-2\"}}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"in segment maur-2\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"default rule\"}]}", featureFlag);
    }
}