package io.split.client;

import java.util.concurrent.TimeoutException;

/**
 * Created by patricioe on 9/20/17.
 */
public class TestProxyMain {

    public static void main(String[] args) throws Exception {

        SplitClientConfig config = SplitClientConfig.builder()
                .ready(6 * 60 * 1000)
                .proxyHost("172.17.0.11")
                .proxyPort(3128)
                .proxyUsername("foo")
                .proxyPassword("bar")
                .enableDebug()
                .build();


        SplitClient client = null;
        SplitManager manager = null;
        try {
            SplitFactory factory = SplitFactoryBuilder.build("r2tdmvojm6rp6vugmd9svhoedv43e71ejolk", config);
            client = factory.client();
            manager = factory.manager();
        } catch (TimeoutException t) {
            System.exit(-1);
        }


        while (true) {
            System.out.println(client.getTreatment("pato", "erase_me"));
            Thread.sleep(1000);
        }

    }
}
