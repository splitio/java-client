package io.split.client.utils;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 6/10/16.
 */
public class Utils {

    public static HttpEntity toJsonEntity(Object obj) {
        String json = Json.toJson(obj);
        return HttpEntities.create(json, ContentType.APPLICATION_JSON);
    }


    public static void forceClose(CloseableHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static URI appendPath(URI root, String pathToAppend) throws URISyntaxException {
        checkNotNull(root);
        checkNotNull(pathToAppend);
        //Add or not the backslash depending on whether the roots ends with / or not
        String path = String.format("%s%s%s", root.getPath(), root.getPath().endsWith("/") ? "" : "/", pathToAppend);
        return new URIBuilder(root).setPath(path).build();
    }
}