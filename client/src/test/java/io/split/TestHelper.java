package io.split;

import io.split.client.dtos.Condition;
import io.split.client.dtos.Excluded;
import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.Status;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeader;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestHelper {
    public static CloseableHttpClient mockHttpClient(String jsonName, int httpStatus) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        HttpEntity entityMock = Mockito.mock(HttpEntity.class);
        Mockito.when(entityMock.getContent()).thenReturn(TestHelper.class.getClassLoader().getResourceAsStream(jsonName));

        ClassicHttpResponse httpResponseMock = Mockito.mock(ClassicHttpResponse.class);
        Mockito.when(httpResponseMock.getEntity()).thenReturn(entityMock);
        Mockito.when(httpResponseMock.getCode()).thenReturn(httpStatus);
        Header[] headers = new Header[2];
        headers[0] = new BasicHeader(HttpHeaders.VIA, "HTTP/1.1 m_proxy_rio1");
        headers[1] = new BasicHeader(HttpHeaders.VIA, "HTTP/1.1 s_proxy_rio1");
        Mockito.when(httpResponseMock.getHeaders()).thenReturn(headers);
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(httpClientMock.execute(Mockito.anyObject())).thenReturn(classicResponseToCloseableMock(httpResponseMock));

        return httpClientMock;
    }

    public static CloseableHttpResponse classicResponseToCloseableMock(ClassicHttpResponse mocked) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method adaptMethod = CloseableHttpResponse.class.getDeclaredMethod("adapt", ClassicHttpResponse.class);
        adaptMethod.setAccessible(true);
        return (CloseableHttpResponse) adaptMethod.invoke(null, mocked);
    }

    public static RuleBasedSegment makeRuleBasedSegment(String name, List<Condition> conditions, long changeNumber) {
        Excluded excluded = new Excluded();
        excluded.segments = new ArrayList<>();
        excluded.keys = new ArrayList<>();

        RuleBasedSegment ruleBasedSegment = new RuleBasedSegment();
        ruleBasedSegment.name = name;
        ruleBasedSegment.status = Status.ACTIVE;
        ruleBasedSegment.conditions = conditions;
        ruleBasedSegment.trafficTypeName = "user";
        ruleBasedSegment.changeNumber = changeNumber;
        ruleBasedSegment.excluded = excluded;
        return ruleBasedSegment;
    }

}
