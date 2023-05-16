package io.split.client;

import io.split.client.api.Key;
import io.split.client.api.SplitResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adilaijaz on 5/8/15.
 */
public interface SplitClient {

    /**
     * Returns the treatment to show this key for this feature flag. The set of treatments
     * for a feature flag can be configured on the Split user interface.
     * <p/>
     * <p/>
     * This method returns the string 'control' if:
     * <ol>
     * <li>Any of the parameters were null</li>
     * <li>There was an exception in evaluating the treatment</li>
     * <li>The SDK does not know of the existence of this feature flag</li>
     * <li>The feature flag was deleted through the Split user interface.</li>
     * </ol>
     * 'control' is a reserved treatment (you cannot create a treatment with the
     * same name) to highlight these exceptional circumstances.
     * <p/>
     * <p/>
     * The sdk returns the default treatment of this feature flag if:
     * <ol>
     * <li>The feature flag was killed</li>
     * <li>The key did not match any of the conditions in the feature flag roll-out plan</li>
     * </ol>
     * The default treatment of a feature flag is set on the Split user interface.
     * <p/>
     * <p/>
     * This method does not throw any exceptions. It also never returns null.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagName the name of the feature flag we want to evaluate. MUST NOT be null.
     * @return the evaluated treatment, the default treatment of this feature flag, or 'control'.
     */
    String getTreatment(String key, String featureFlagName);

    /**
     * This method is useful when you want to determine the treatment to show
     * to an customer (user, account etc.) based on an attribute of that customer
     * instead of it's key.
     * <p/>
     * <p/>
     * Examples include showing a different treatment to users on trial plan
     * vs. premium plan. Another example is to show a different treatment
     * to users created after a certain date.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagName the name of the feature flag we want to evaluate. MUST NOT be null.
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return the evaluated treatment, the default treatment of this feature flag, or 'control'.
     */
    String getTreatment(String key, String featureFlagName, Map<String, Object> attributes);

    /**
     * To understand why this method is useful, consider the following simple Feature Flag as an example:
     *
     * if user is in segment employees then feature flag 100%:on
     * else if user is in segment all then feature flag 20%:on,80%:off
     *
     * There are two concepts here: matching and bucketing. Matching
     * refers to ‘user is in segment employees’ or ‘user is in segment
     * all’ whereas bucketing refers to ‘100%:on’ or ‘20%:on,80%:off’.
     *
     * By default, the same customer key is used for both matching and
     * bucketing. However, for some advanced use cases, you may want
     * to use different keys. For such cases, use this method.
     *
     * As an example, suppose you want to rollout to percentages of
     * users in specific accounts. You can achieve that by matching
     * via account id, but bucketing by user id.
     *
     * Another example is when you want to ensure that a user continues to get
     * the same treatment after they sign up for your product that they used
     * to get when they were simply a visitor to your site. In that case,
     * before they sign up, you can use their visitor id for both matching and bucketing, but
     * post log-in you can use their user id for matching and visitor id for bucketing.
     *
     *
     * @param key the matching and bucketing keys. MUST NOT be null.
     * @param featureFlagName the name of the feature flag we want to evaluate. MUST NOT be null.
     * @param attributes of the entity (user, account etc.) to use in evaluation. Can be null or empty.
     *
     * @return the evaluated treatment, the default treatment of this feature flag, or 'control'.
     */
    String getTreatment(Key key, String featureFlagName, Map<String, Object> attributes);

    /**
     * Same as {@link #getTreatment(String, String)} but it returns the configuration associated to the
     * matching treatment if any. Otherwise {@link SplitResult.configurations()} will be null.
     * <p/>
     * <p/>
     * Examples include showing a different treatment to users on trial plan
     * vs. premium plan. Another example is to show a different treatment
     * to users created after a certain date.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagName the name of the feature flag we want to evaluate. MUST NOT be null.
     * @return SplitResult containing the evaluated treatment (the default treatment of this feature flag, or 'control') and
     *         a configuration associated to this treatment if set.
     */
    SplitResult getTreatmentWithConfig(String key, String featureFlagName);

    /**
     * Same as {@link #getTreatment(String, String, Map)} but it returns the configuration associated to the
     * matching treatment if any. Otherwise {@link SplitResult.configurations()} will be null.
     * <p/>
     * <p/>
     * Examples include showing a different treatment to users on trial plan
     * vs. premium plan. Another example is to show a different treatment
     * to users created after a certain date.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagName the name of the feature flag we want to evaluate. MUST NOT be null.
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return SplitResult containing the evaluated treatment (the default treatment of this feature flag, or 'control') and
     *         a configuration associated to this treatment if set.
     */
    SplitResult getTreatmentWithConfig(String key, String featureFlagName, Map<String, Object> attributes);

    /**
     * Same as {@link #getTreatment(Key, String, Map)} but it returns the configuration associated to the
     * matching treatment if any. Otherwise {@link SplitResult.configurations()} will be null.
     *
     * @param key the matching and bucketing keys. MUST NOT be null.
     * @param featureFlagName the name of the feature flag we want to evaluate. MUST NOT be null.
     * @param attributes of the entity (user, account etc.) to use in evaluation. Can be null or empty.
     *
     * @return SplitResult containing the evaluated treatment (the default treatment of this feature flag, or 'control') and
     *         a configuration associated to this treatment if set.
     */
    SplitResult getTreatmentWithConfig(Key key, String featureFlagName, Map<String, Object> attributes);

    /**
     * Returns a map of feature flag name and treatments to show this key for these feature flags. The set of treatments
     * for a feature flag can be configured on the Split user interface.
     * <p/>
     * <p/>
     * This method returns for each feature flag the string 'control' if:
     * <ol>
     * <li>Any of the parameters were null</li>
     * <li>There was an exception in evaluating the treatment</li>
     * <li>The SDK does not know of the existence of this feature flag</li>
     * <li>The feature flag was deleted through the Split user interface.</li>
     * </ol>
     * 'control' is a reserved treatment (you cannot create a treatment with the
     * same name) to highlight these exceptional circumstances.
     * <p/>
     * <p/>
     * The sdk returns for each feature flag the default treatment of this feature flag if:
     * <ol>
     * <li>The feature flag was killed</li>
     * <li>The key did not match any of the conditions in the feature flag roll-out plan</li>
     * </ol>
     * The default treatment of a feature flag is set on the Split user interface.
     * <p/>
     * <p/>
     * This method does not throw any exceptions. It also never returns null.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagNames the names of the feature flags we want to evaluate. MUST NOT be null.
     * @return for each feature flag the evaluated treatment, the default treatment for each feature flag, or 'control'.
     */
    Map<String, String> getTreatments(String key, List<String> featureFlagNames);

    /**
     * This method is useful when you want to determine the treatments to show
     * to a customer (user, account etc.) based on an attribute of that customer
     * instead of their key.
     * <p/>
     * <p/>
     * Examples include showing different treatments to users on trial plan
     * vs. premium plan. Another example is to show different treatments
     * to users created after a certain date.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagNames the names of the feature flags we want to evaluate. MUST NOT be null.
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return the evaluated treatment, the default treatment of this feature flag, or 'control'.
     */
    Map<String, String> getTreatments(String key, List<String> featureFlagNames, Map<String, Object> attributes);

    /**
     * To understand why this method is useful, consider the following simple Feature Flag as an example:
     *
     * if user is in segment employees then feature flag 100%:on
     * else if user is in segment all then feature flag 20%:on,80%:off
     *
     * There are two concepts here: matching and bucketing. Matching
     * refers to ‘user is in segment employees’ or ‘user is in segment
     * all’ whereas bucketing refers to ‘100%:on’ or ‘20%:on,80%:off’.
     *
     * By default, the same customer key is used for both matching and
     * bucketing. However, for some advanced use cases, you may want
     * to use different keys. For such cases, use this method.
     *
     * As an example, suppose you want to rollout to percentages of
     * users in specific accounts. You can achieve that by matching
     * via account id, but bucketing by user id.
     *
     * Another example is when you want to ensure that a user continues to get
     * the same treatment after they sign up for your product that they used
     * to get when they were simply a visitor to your site. In that case,
     * before they sign up, you can use their visitor id for both matching and bucketing, but
     * post log-in you can use their user id for matching and visitor id for bucketing.
     *
     *
     * @param key the matching and bucketing keys. MUST NOT be null.
     * @param featureFlagNames the names of the feature flags we want to evaluate. MUST NOT be null.
     * @param attributes of the entity (user, account etc.) to use in evaluation. Can be null or empty.
     *
     * @return for each feature flag the evaluated treatment, the default treatment of the feature flag, or 'control'.
     */
    Map<String, String> getTreatments(Key key, List<String> featureFlagNames, Map<String, Object> attributes);

    /**
     * Same as {@link #getTreatments(String, List<String>)} but it returns the configuration associated to the
     * matching treatments if any. Otherwise {@link SplitResult.configurations()} will be null.
     * <p/>
     * <p/>
     * Examples include showing a different treatment to users on trial plan
     * vs. premium plan. Another example is to show a different treatment
     * to users created after a certain date.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagNames the names of the feature flags we want to evaluate. MUST NOT be null.
     * @return Map<String, SplitResult> containing for each feature flag the evaluated treatment (the default treatment of this feature flag, or 'control') and
     *         a configuration associated to this treatment if set.
     */
    Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames);

    /**
     * Same as {@link #getTreatments(String, List<String>, Map)} but it returns for each feature flag the configuration associated to the
     * matching treatment if any. Otherwise {@link SplitResult.configurations()} will be null.
     * <p/>
     * <p/>
     * Examples include showing a different treatment to users on trial plan
     * vs. premium plan. Another example is to show a different treatment
     * to users created after a certain date.
     *
     * @param key a unique key of your customer (e.g. user_id, user_email, account_id, etc.) MUST not be null.
     * @param featureFlagNames the names of the feature flags we want to evaluate. MUST NOT be null.
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return for each feature flag a SplitResult containing the evaluated treatment (the default treatment of this feature flag, or 'control') and
     *         a configuration associated to this treatment if set.
     */
    Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, Map<String, Object> attributes);

    /**
     * Same as {@link #getTreatments(Key, List<String>, Map)} but it returns for each feature flag the configuration associated to the
     * matching treatment if any. Otherwise {@link SplitResult.configurations()} will be null.
     *
     * @param key the matching and bucketing keys. MUST NOT be null.
     * @param featureFlagNames the names of the feature flags we want to evaluate. MUST NOT be null.
     * @param attributes of the entity (user, account etc.) to use in evaluation. Can be null or empty.
     *
     * @return for each feature flag a SplitResult containing the evaluated treatment (the default treatment of this feature flag, or 'control') and
     *         a configuration associated to this treatment if set.
     */
    Map<String, SplitResult> getTreatmentsWithConfig(Key key, List<String> featureFlagNames, Map<String, Object> attributes);

    /**
     * Destroys the background processes and clears the cache, releasing the resources used by
     * the any instances of SplitClient or SplitManager generated by the client's parent SplitFactory
     */
    void destroy();

    /**
     * Enqueue a new event to be sent to split data collection services
     *
     * Example:
     *      client.track(“account”, “Split Software”, “checkout”)
     *
     * @param key the identifier of the entity
     * @param trafficType the type of the event
     * @param eventType the type of the event
     *
     * @return true if the track was successful, false otherwise
     */
    boolean track(String key, String trafficType, String eventType);

    /**
     * Enqueue a new event to be sent to split data collection services
     *
     * Example:
     *      client.track(“account”, “Split Software”, “checkout”, 200.00)
     *
     * @param key the identifier of the entity
     * @param trafficType the type of the event
     * @param eventType the type of the event
     * @param value the value of the event
     *
     * @return true if the track was successful, false otherwise
     */
    boolean track(String key, String trafficType, String eventType, double value);

    /**
     * Enqueue a new event to be sent to split data collection services
     *
     * Example:
     *      client.track(“account”, “Split Software”, “checkout”, Collections.singletonMap("age", 23))
     *
     * @param key the identifier of the entity
     * @param trafficType the type of the event
     * @param eventType the type of the event
     * @param value the value of the event
     *
     * @return true if the track was successful, false otherwise
     */
    boolean track(String key, String trafficType, String eventType, Map<String, Object> properties);

    /**
     * Enqueue a new event to be sent to split data collection services
     *
     * Example:
     *      client.track(“account”, “Split Software”, “checkout”, 123, Collections.singletonMap("age", 23))
     *
     * @param key the identifier of the entity
     * @param trafficType the type of the event
     * @param eventType the type of the event
     * @param value the value of the event
     *
     * @return true if the track was successful, false otherwise
     */
    boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties);


    /**
     * The SDK kicks off background threads to download data necessary
     * for using the SDK. You can choose to block until the SDK has
     * downloaded split definitions so that you will not get
     * the 'control' treatment.
     * <p>
     *
     * If the download is not successful in the time period set on
     * {@link SplitClientConfig.Builder#setBlockUntilReadyTimeout}, a TimeoutException will be thrown.
     * <p>
     */
    void blockUntilReady() throws TimeoutException, InterruptedException;
}