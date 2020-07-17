package io.split.engine.sse;

import com.google.gson.Gson;
import io.split.engine.sse.dtos.*;
import io.split.engine.sse.exceptions.EventParsingException;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationParserImp implements NotificationParser {
    private static final String OCCUPANCY_PREFIX = "[?occupancy=metrics.publishers]";

    private final Gson _gson;

    public NotificationParserImp(Gson gson) {
        _gson = checkNotNull(gson);
    }

    @Override
    public IncomingNotification parseMessage(String payload) throws EventParsingException {
        try {
            RawMessageNotification rawMessageNotification = _gson.fromJson(payload, RawMessageNotification.class);
            GenericNotificationData genericNotificationData = _gson.fromJson(rawMessageNotification.getData(), GenericNotificationData.class);
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
            ErrorNotification messageError = _gson.fromJson(payload, ErrorNotification.class);

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
                return new SplitChangeNotification(genericNotificationData);
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
