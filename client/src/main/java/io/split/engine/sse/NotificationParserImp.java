package io.split.engine.sse;

import io.split.client.utils.Json;

import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.OccupancyNotification;
import io.split.engine.sse.dtos.RawMessageNotification;
import io.split.engine.sse.dtos.SegmentChangeNotification;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.engine.sse.exceptions.EventParsingException;

public class NotificationParserImp implements NotificationParser {
    private static final String OCCUPANCY_PREFIX = "[?occupancy=metrics.publishers]";

    @Override
    public IncomingNotification parseMessage(String payload) throws EventParsingException {
        try {
            RawMessageNotification rawMessageNotification = Json.fromJson(payload, RawMessageNotification.class);
            GenericNotificationData genericNotificationData = Json.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
            genericNotificationData.setChannel(rawMessageNotification.getChannel());
            if (rawMessageNotification.getChannel().contains(OCCUPANCY_PREFIX)) {
                return parseControlChannelMessage(genericNotificationData);
            }
            return parseNotification(genericNotificationData);
        } catch (Exception ex) {
            throw new EventParsingException("Error parsing event.", ex, payload);
        }
    }

    @Override
    public ErrorNotification parseError(String payload) throws EventParsingException {
        try {
            ErrorNotification messageError = Json.fromJson(payload, ErrorNotification.class);
            if (messageError.getMessage() == null || messageError.getStatusCode() == null) {
                throw new Exception("Wrong notification format.");
            }
            return messageError;
        } catch (Exception ex) {
            throw new EventParsingException("Error parsing event.", ex, payload);
        }
    }

    private IncomingNotification parseNotification(GenericNotificationData genericNotificationData) throws Exception {
        switch (genericNotificationData.getType()) {
            case SPLIT_UPDATE:
                return new FeatureFlagChangeNotification(genericNotificationData);
            case SPLIT_KILL:
                return new SplitKillNotification(genericNotificationData);
            case SEGMENT_UPDATE:
                return new SegmentChangeNotification(genericNotificationData);
            default:
                throw new Exception("Wrong Notification type.");
        }
    }

    private IncomingNotification parseControlChannelMessage(GenericNotificationData genericNotificationData) {
        String channel = genericNotificationData.getChannel().replace(OCCUPANCY_PREFIX, "");
        genericNotificationData.setChannel(channel);

        if (genericNotificationData.getControlType() != null) {
            return new ControlNotification(genericNotificationData);
        }
        return new OccupancyNotification(genericNotificationData);
    }
}