package io.split.client.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpRequest;

import io.split.client.RequestDecorator;
import io.split.client.dtos.RequestContext;

public class ApacheRequestDecorator {

    public static HttpRequest decorate(HttpRequest request, RequestDecorator decorator) {

        RequestContext ctx = new RequestContext(convertToMap(request.getHeaders()));
        for (Map.Entry<String, List<String>> entry : decorator.decorateHeaders(ctx).headers().entrySet()) {
            List<String> values = entry.getValue();
            for (int i = 0; i < values.size(); i++) {
                if (i == 0) {
                    request.setHeader(entry.getKey(), values.get(i));
                } else {
                    request.addHeader(entry.getKey(), values.get(i));
                }
            }
        }

        return request;
    }

    private static Map<String, List<String>> convertToMap(Header[] to_convert) {
        Map<String, List<String>> to_return = new HashMap<String, List<String>>();
        for (Integer i = 0; i < to_convert.length; i++) {
            if (!to_return.containsKey(to_convert[i].getName())) {
                to_return.put(to_convert[i].getName(), new ArrayList<String>());
            }
            to_return.get(to_convert[i].getName()).add(to_convert[i].getValue());
        }
        return to_return;
    }
}
