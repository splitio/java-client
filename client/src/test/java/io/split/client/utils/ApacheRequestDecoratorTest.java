package io.split.client.utils;

import io.split.client.CustomHeaderDecorator;
import io.split.client.RequestDecorator;
import io.split.client.dtos.RequestContext;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Arrays;
import java.util.Map;

public class ApacheRequestDecoratorTest {

    @Test
    public void testNoOp() {
        ApacheRequestDecorator apacheRequestDecorator = new ApacheRequestDecorator();
        RequestDecorator requestDecorator = new RequestDecorator(null);
        HttpGet request = new HttpGet("http://anyhost");

        request  = (HttpGet) apacheRequestDecorator.decorate(request, requestDecorator);
        Assert.assertEquals(0, request.getHeaders().length);
        request.addHeader("myheader", "value");
        request  = (HttpGet) apacheRequestDecorator.decorate(request, requestDecorator);
        Assert.assertEquals(1, request.getHeaders().length);
    }

    @Test
    public void testAddCustomHeaders() throws ProtocolException {
        class MyCustomHeaders implements CustomHeaderDecorator {
            public MyCustomHeaders() {}
            @Override
            public Map<String, List<String>> getHeaderOverrides(RequestContext context) {
                Map<String, List<String>> additionalHeaders = context.headers();
                additionalHeaders.put("first", Arrays.asList("1"));
                additionalHeaders.put("second", Arrays.asList("2.1", "2.2"));
                additionalHeaders.put("third", Arrays.asList("3"));
                return additionalHeaders;
            }
        }
        MyCustomHeaders myHeaders = new MyCustomHeaders();
        RequestDecorator decorator = new RequestDecorator(myHeaders);
        ApacheRequestDecorator apacheRequestDecorator = new ApacheRequestDecorator();

        HttpGet request = new HttpGet("http://anyhost");
        request.addHeader("first", "myfirstheader");
        request  = (HttpGet) apacheRequestDecorator.decorate(request, decorator);

        Assert.assertEquals(4, request.getHeaders().length);
        Assert.assertEquals("1", request.getHeader("first").getValue());

        Header[] second = request.getHeaders("second");
        Assert.assertEquals("2.1", second[0].getValue());
        Assert.assertEquals("2.2", second[1].getValue());
        Assert.assertEquals("3", request.getHeader("third").getValue());

        HttpPost request2 = new HttpPost("http://anyhost");
        request2.addHeader("myheader", "value");
        request2  = (HttpPost) apacheRequestDecorator.decorate(request2, decorator);
        Assert.assertEquals(5, request2.getHeaders().length);
    }

    @Test
    public void testAddBlockedHeaders() throws ProtocolException {
        class MyCustomHeaders implements  CustomHeaderDecorator {
            public MyCustomHeaders() {}
            @Override
            public Map<String, List<String>> getHeaderOverrides(RequestContext context) {
                Map<String, List<String>> additionalHeaders = context.headers();
                additionalHeaders.put("first", Arrays.asList("1"));
                additionalHeaders.put("SplitSDKVersion", Arrays.asList("2.4"));
                additionalHeaders.put("SplitMachineip", Arrays.asList("xx"));
                additionalHeaders.put("splitMachineName", Arrays.asList("xx"));
                additionalHeaders.put("splitimpressionsmode", Arrays.asList("xx"));
                additionalHeaders.put("HOST", Arrays.asList("xx"));
                additionalHeaders.put("referrer", Arrays.asList("xx"));
                additionalHeaders.put("content-type", Arrays.asList("xx"));
                additionalHeaders.put("content-length", Arrays.asList("xx"));
                additionalHeaders.put("content-encoding", Arrays.asList("xx"));
                additionalHeaders.put("ACCEPT", Arrays.asList("xx"));
                additionalHeaders.put("keep-alive", Arrays.asList("xx"));
                additionalHeaders.put("x-fastly-debug", Arrays.asList("xx"));
                return additionalHeaders;
            }
        }
        MyCustomHeaders myHeaders = new MyCustomHeaders();
        RequestDecorator decorator = new RequestDecorator(myHeaders);
        ApacheRequestDecorator apacheRequestDecorator = new ApacheRequestDecorator();
        HttpGet request = new HttpGet("http://anyhost");
        request  = (HttpGet) apacheRequestDecorator.decorate(request, decorator);
        Assert.assertEquals(1, request.getHeaders().length);
        Assert.assertEquals(null, request.getHeader("SplitSDKVersion"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void customDecoratorError() {
        class MyCustomHeaders implements  CustomHeaderDecorator {
            public MyCustomHeaders() {}
            @Override
            public Map<String, List<String>> getHeaderOverrides(RequestContext context) {
                throw new RuntimeException();
            }
        }
        MyCustomHeaders myHeaders = new MyCustomHeaders();
        RequestDecorator decorator = new RequestDecorator(myHeaders);
        ApacheRequestDecorator apacheRequestDecorator = new ApacheRequestDecorator();
        HttpGet request = new HttpGet("http://anyhost");
        request  = (HttpGet) apacheRequestDecorator.decorate(request, decorator);
    }
}