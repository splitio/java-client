package io.split.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

/**
 * Created by patricioe on 5/3/17.
 */
public class MainTest {
    public static void main(String[] args) throws InterruptedException, URISyntaxException, TimeoutException, IOException {

        SplitClient sdk = SplitFactoryBuilder.build("42fsieif0v7d9jqm3qrqn59bdn",
                SplitClientConfig.builder().ready(10000).build()).client();

        while (true) {
            System.out.println(sdk.getTreatment("pato","Groups_UI"));
            Thread.sleep(2000);
        }
    }
}
