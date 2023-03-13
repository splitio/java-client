package io.split.client;

import io.split.inputValidation.ApiKeyValidator;
import io.split.grammar.Treatments;
import io.split.storages.enums.StorageMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * Builds an instance of SplitClient.
 */
public class SplitFactoryBuilder {
    private static final Logger _log = LoggerFactory.getLogger(SplitFactoryBuilder.class);

    /**
     * Instantiates a SplitFactory with default config
     *
     * @param apiToken the API token. MUST NOT be null
     * @return a SplitFactory
     * @throws IOException                           if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     */
    public static SplitFactory build(String apiToken) throws IOException, URISyntaxException {
        return build(apiToken, SplitClientConfig.builder().build());
    }

    /**
     * @param apiToken the API token. MUST NOT be null
     * @param config   parameters to control sdk construction. MUST NOT be null.
     * @return a SplitFactory
     * @throws java.io.IOException                   if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     */
    public static synchronized SplitFactory build(String apiToken, SplitClientConfig config) throws IOException, URISyntaxException {
        ApiKeyValidator.validate(apiToken);
        String splitFile = config.splitFile();
        if (LocalhostSplitFactory.LOCALHOST.equals(apiToken)) {
            if (splitFile != null && splitFile.toLowerCase().endsWith(".json")){
                return new SplitFactoryImpl(config);
            }
            return LocalhostSplitFactory.createLocalhostSplitFactory(config);
        }
        if (StorageMode.PLUGGABLE.equals(config.storageMode()) || StorageMode.REDIS.equals(config.storageMode())){
            return new SplitFactoryImpl(apiToken, config, config.customStorageWrapper());
        }
        return new SplitFactoryImpl(apiToken, config);
    }

    /**
     * Instantiates a local Off-The-Grid SplitFactory
     *
     * @throws IOException if there were problems reading the override file from disk.
     */
    public static SplitFactory local() throws IOException, URISyntaxException {
        return LocalhostSplitFactory.createLocalhostSplitFactory(SplitClientConfig.builder().build());
    }

    /**
     * Instantiates a local Off-The-Grid SplitFactory
     *
     * @return config Split config file
     * @throws IOException if there were problems reading the override file from disk.
     */
    public static SplitFactory local(SplitClientConfig config) throws IOException, URISyntaxException {
        return LocalhostSplitFactory.createLocalhostSplitFactory(config);
    }

    public static void main(String... args) throws IOException, URISyntaxException {
        if (args.length != 1) {
            System.out.println("Usage: <api_token>");
            System.exit(1);
            return;
        }

        SplitClientConfig config = SplitClientConfig.builder().build();
        SplitClient client = SplitFactoryBuilder.build("API_KEY", config).client();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if ("exit".equals(line)) {
                    System.exit(0);
                }
                String[] userIdAndSplit = line.split(" ");

                if (userIdAndSplit.length != 2) {
                    System.out.println("Could not understand command");
                    continue;
                }

                boolean isOn = client.getTreatment(userIdAndSplit[0], userIdAndSplit[1]).equals("on");

                System.out.println(isOn ? Treatments.ON : Treatments.OFF);
            }
        } catch (IOException io) {
            _log.error(io.getMessage(), io);
        }
    }
}
