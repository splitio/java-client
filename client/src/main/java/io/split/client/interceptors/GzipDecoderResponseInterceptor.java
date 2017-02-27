package io.split.client.interceptors;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by adilaijaz on 5/22/15.
 */
public class GzipDecoderResponseInterceptor implements HttpResponseInterceptor {

    @Override
    public void process(HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            Header ceheader = entity.getContentEncoding();
            if (ceheader != null) {
                HeaderElement[] codecs = ceheader.getElements();
                for (int i = 0; i < codecs.length; i++) {
                    if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                        response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                        return;
                    }
                }
            }
        }
    }
}
