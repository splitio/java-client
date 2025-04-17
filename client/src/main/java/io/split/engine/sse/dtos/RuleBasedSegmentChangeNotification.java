package io.split.engine.sse.dtos;

import io.split.client.dtos.RuleBasedSegment;
import io.split.client.utils.Json;
import io.split.engine.sse.NotificationProcessor;

import java.io.UnsupportedEncodingException;

public class RuleBasedSegmentChangeNotification extends CommonChangeNotification {
    private RuleBasedSegment ruleBasedSegmentDefinition;

    public RuleBasedSegmentChangeNotification(GenericNotificationData genericNotificationData) {
        super(genericNotificationData, Type.RB_SEGMENT_UPDATE);
    }

    public RuleBasedSegment getRuleBasedSegmentDefinition() {
        return ruleBasedSegmentDefinition;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processRuleBasedSegmentUpdate(this);
    }

    @Override
    public void updateDefinition(byte[] decodedBytes) throws UnsupportedEncodingException {
        ruleBasedSegmentDefinition = Json.fromJson(new String(decodedBytes, 0, decodedBytes.length, "UTF-8"), RuleBasedSegment.class);
    }
}