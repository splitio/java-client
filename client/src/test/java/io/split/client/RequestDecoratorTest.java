package io.split.client;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RequestDecoratorTest {

    @Test
    public void testNoOp() {
        RequestDecorator decorator = new RequestDecorator(null);
        HttpGet request = new HttpGet("http://anyhost");
        request  = (HttpGet) decorator.decorateHeaders(request);
        Assert.assertEquals(0, request.getHeaders().length);
        request.addHeader("myheader", "value");
        request  = (HttpGet) decorator.decorateHeaders(request);
        Assert.assertEquals(1, request.getHeaders().length);
    }

    @Test
    public void testAddCustomHeaders() throws ProtocolException {
        class MyCustomHeaders implements  UserCustomHeaderDecorator {
            public MyCustomHeaders() {}
            @Override
            public Map<String, String> getHeaderOverrides() {
                return new HashMap<String, String>()
                {{
                    put("first", "1");
                    put("second", "2");
                    put("third", "3");
                }};
            }
        }
        MyCustomHeaders myHeaders = new MyCustomHeaders();
        RequestDecorator decorator = new RequestDecorator(myHeaders);
        HttpGet request = new HttpGet("http://anyhost");
        request  = (HttpGet) decorator.decorateHeaders(request);
        Assert.assertEquals(3, request.getHeaders().length);
        Assert.assertEquals("1", request.getHeader("first").getValue());
        Assert.assertEquals("2", request.getHeader("second").getValue());
        Assert.assertEquals("3", request.getHeader("third").getValue());

        HttpPost request2 = new HttpPost("http://anyhost");
        request2.addHeader("myheader", "value");
        request2  = (HttpPost) decorator.decorateHeaders(request2);
        Assert.assertEquals(4, request2.getHeaders().length);
    }

    @Test
    public void testAddBlockedHeaders() throws ProtocolException {
        class MyCustomHeaders implements  UserCustomHeaderDecorator {
            public MyCustomHeaders() {}
            @Override
            public Map<String, String> getHeaderOverrides() {
                return new HashMap<String, String>()
                {{
                    put("first", "1");
                    put("SplitSDKVersion", "2.4");
                    put("SplitMachineip", "xx");
                    put("splitMachineName", "xx");
                    put("splitimpressionsmode", "xx");
                    put("HOST", "xx");
                    put("referrer", "xx");
                    put("content-type", "xx");
                    put("content-length", "xx");
                    put("content-encoding", "xx");
                    put("ACCEPT", "xx");
                    put("keep-alive", "xx");
                    put("x-fastly-debug", "xx");

                }};
            }
        }
        MyCustomHeaders myHeaders = new MyCustomHeaders();
        RequestDecorator decorator = new RequestDecorator(myHeaders);
        HttpGet request = new HttpGet("http://anyhost");
        request  = (HttpGet) decorator.decorateHeaders(request);
        Assert.assertEquals(1, request.getHeaders().length);
        Assert.assertEquals(null, request.getHeader("SplitSDKVersion"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void customDecoratorError() {
        class MyCustomHeaders implements  UserCustomHeaderDecorator {
            public MyCustomHeaders() {}
            @Override
            public Map<String, String> getHeaderOverrides() {
                throw new RuntimeException();
            }
        }
        MyCustomHeaders myHeaders = new MyCustomHeaders();
        RequestDecorator decorator = new RequestDecorator(myHeaders);
        HttpGet request = new HttpGet("http://anyhost");
        request  = (HttpGet) decorator.decorateHeaders(request);
    }
}