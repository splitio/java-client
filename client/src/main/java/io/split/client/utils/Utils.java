package io.split.client.utils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 6/10/16.
 */
public class Utils {

    public static StringEntity toJsonEntity(Object obj) {
        String json = Json.toJson(obj);
        StringEntity entity = new StringEntity(json, "UTF-8");
        entity.setContentType("application/json");
        return entity;
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

    public static void wah() throws URISyntaxException {
        java.net.URI uri = new java.net.URI ("http://www.baseaddress.com/path/index1.html");
        System.out.println (uri.resolve ("index2.html"));
        System.out.println (uri.resolve ("/path2/"));
    }

}
