package io.split;

import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class App {

    public static void main(String[] args) throws InterruptedException, TimeoutException, IOException, URISyntaxException {

        SplitClientConfig cfg = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(20000)
                .endpoint("https://sdk.split-stage.io",
                        "http://events.split-stage.io")
                .build();
        SplitFactory factory = SplitFactoryBuilder.build("uq5onn4o022c8okecdamennf1t8lt0a87lfq", cfg);
        SplitClient c = factory.client();
        c.blockUntilReady();

        while(true) {
            Thread.sleep(1000);
        }



    }
}
