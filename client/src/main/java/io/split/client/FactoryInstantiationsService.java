package io.split.client;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactoryInstantiationsService {

    private static final Logger _log = LoggerFactory.getLogger(FactoryInstantiationsService.class);
    private static volatile FactoryInstantiationsService _factoryInstantiationsService;
    private static final Multiset<String> USED_API_TOKENS = ConcurrentHashMultiset.create();


    private FactoryInstantiationsService() {}

    public static FactoryInstantiationsService getFactoryInstantiationsServiceInstance() {
        if(_factoryInstantiationsService == null) {
            synchronized (FactoryInstantiationsService.class) {
                if (_factoryInstantiationsService == null) {
                    _factoryInstantiationsService = new FactoryInstantiationsService();
                }
            }
        }

        return _factoryInstantiationsService;
    }

    public void addToken(String apiToken) {
        String message;
        if (USED_API_TOKENS.contains(apiToken)) {
            message = String.format("factory instantiation: You already have %s with this API Key. " +
                            "We recommend keeping only one instance of the factory at all times (Singleton pattern) and reusing " +
                            "it throughout your application.",
                    USED_API_TOKENS.count(apiToken) == 1 ? "1 factory" : String.format("%s factories", USED_API_TOKENS.count(apiToken)));
            _log.warn(message);
        } else if (!USED_API_TOKENS.isEmpty()) {
             message = "factory instantiation: You already have an instance of the Split factory. " +
                    "Make sure you definitely want this additional instance. We recommend keeping only one instance of " +
                    "the factory at all times (Singleton pattern) and reusing it throughout your application.“";
            _log.warn(message);
        }
        USED_API_TOKENS.add(apiToken);
    }

    public void removeToken(String apiToken) {
        USED_API_TOKENS.remove(apiToken);
    }

    /**
     * Just for test
     * @param apiToken
     * @return
     */
    public boolean isTokenPresent(String apiToken){
        return USED_API_TOKENS.contains(apiToken);
    }
}
