package io.split.engine.sse.dtos;

import io.split.client.dtos.Split;
import io.split.client.utils.Json;
import io.split.engine.sse.NotificationProcessor;

import java.io.UnsupportedEncodingException;

public class FeatureFlagChangeNotification extends CommonChangeNotification {
    private Split featureFlagDefinition;

    public FeatureFlagChangeNotification(GenericNotificationData genericNotificationData) {
        super(genericNotificationData, Type.SPLIT_UPDATE);
    }

    public Split getFeatureFlagDefinition() {
        return featureFlagDefinition;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processSplitUpdate(this);
    }

    @Override
    public void updateDefinition(byte[] decodedBytes) throws UnsupportedEncodingException {
        featureFlagDefinition = Json.fromJson(new String(decodedBytes, 0, decodedBytes.length, "UTF-8"), Split.class);
    }
}