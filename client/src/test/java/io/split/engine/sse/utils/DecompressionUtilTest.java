package io.split.engine.sse.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import static io.split.engine.sse.utils.DecompressionUtil.gZipDecompress;
import static io.split.engine.sse.utils.DecompressionUtil.zLibDecompress;

public class DecompressionUtilTest {


    @Test
    public void testZLibDecompress() throws UnsupportedEncodingException {
        String toDecode = "eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=";

        byte[] decodedBytes = Base64.getDecoder().decode(toDecode);
        byte[] decompressFeatureFlag = zLibDecompress(decodedBytes);
        String featureFlag = new String(decompressFeatureFlag, 0, decompressFeatureFlag.length, "UTF-8");
        Assert.assertEquals("{\"trafficTypeName\":\"user\",\"id\":\"d431cdd0-b0be-11ea-8a80-1660ada9ce39\",\"name\":\"mauro_java\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-92391491,\"seed\":-1769377604,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1684265694505,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"WHITELIST\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"matcherType\":\"WHITELIST\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"admin\",\"mauro\",\"nico\"]}}]},\"partitions\":[{\"treatment\":\"v5\",\"size\":100}],\"label\":\"whitelisted\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"maur-2\"}}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"in segment maur-2\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"default rule\"}]}", featureFlag);
    }

    @Test
    public void testGZipDecompress() throws UnsupportedEncodingException {
        String toDecode = "H4sIAAAAAAAA/8yT327aTBDFXyU612vJxoTgvUMfKB8qcaSapqoihAZ7DNusvWi9TpUiv3tl/pdQVb1qL+cwc3bOj/EGzlKeq3T6tuaYCoZEXbGFgMogkXXDIM0y31v4C/aCgMnrU9/3gl7Pp4yilMMIAuVusqDamvlXeiWIg/FAa5OSU6aEDHz/ip4wZ5Be1AmjoBsFAtVOCO56UXh31/O7ApUjV1eQGPw3HT+NIPCitG7bctIVC2ScU63d1DK5gksHCZPnEEhXVC45rosFW8ig1++GYej3g85tJEB6aSA7Aqkpc7Ws7XahCnLTbLVM7evnzalsUUHi8//j6WgyTqYQKMilK7b31tRryLa3WKiyfRCDeHhq2Dntiys+JS/J8THUt5VyrFXlHnYTQ3LU2h91yGdQVqhy+0RtTeuhUoNZ08wagTVZdxbBndF5vYVApb7z9m9pZgKaFqwhT+6coRHvg398nEweP/157Bd+S1hz6oxtm88O73B0jbhgM47nyej+YRRfgdNODDlXJWcJL9tUF5SqnRqfbtPr4LdcTHnk4rfp3buLOkG7+Pmp++vRM9w/wVblzX7Pm8OGfxf5YDKZfxh9SS6B/2Pc9t/7ja01o5k1PwIAAP//uTipVskEAAA=";

        byte[] decodedBytes = Base64.getDecoder().decode(toDecode);
        byte[] decompressFeatureFlag = gZipDecompress(decodedBytes);
        String featureFlag = new String(decompressFeatureFlag, 0, decompressFeatureFlag.length, "UTF-8");
        Assert.assertEquals("{\"trafficTypeName\":\"user\",\"id\":\"d431cdd0-b0be-11ea-8a80-1660ada9ce39\",\"name\":\"mauro_java\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-92391491,\"seed\":-1769377604,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1684333081259,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"WHITELIST\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"matcherType\":\"WHITELIST\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"admin\",\"mauro\",\"nico\"]}}]},\"partitions\":[{\"treatment\":\"v5\",\"size\":100}],\"label\":\"whitelisted\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"maur-2\"}}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"in segment maur-2\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100},{\"treatment\":\"V4\",\"size\":0},{\"treatment\":\"v5\",\"size\":0}],\"label\":\"default rule\"}]}", featureFlag);
    }
}