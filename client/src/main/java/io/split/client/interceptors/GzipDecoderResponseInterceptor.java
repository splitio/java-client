package io.split.client.interceptors;


import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by adilaijaz on 5/22/15.
 */
public class GzipDecoderResponseInterceptor implements HttpResponseInterceptor {

    /*
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
*/
    // TODO: Check if this is still necessary
    @Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws HttpException, IOException {

    }
}
