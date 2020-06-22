package io.split.engine.sse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.split.engine.sse.dtos.*;
import io.split.engine.sse.exceptions.EventParsingException;

public class NotificationParserImp implements NotificationParser {
    private static Gson _gson = new Gson();

    @Override
    public IncomingNotification parse(String type, String payload) throws EventParsingException {
        try {
            if (type.equals(StreamingConstants.eventMessage)) {
                MessageNotification messageNotification = _gson.fromJson(payload, (new TypeToken<MessageNotification>() {
                }).getType());

                if (messageNotification.getChannel().contains(StreamingConstants.occupancyPrefix)) {
                    return parseOccupancy(messageNotification);
                }

                return parseNotification(messageNotification);
            } else if (type.equals(StreamingConstants.eventError)) {
                return parseError(payload);
            }

            throw new Exception("Incorrect Notification type.");
        } catch (Exception ex) {
            throw new EventParsingException(ex, payload);
        }
    }

    private IncomingNotification parseNotification(MessageNotification notification) throws Exception {
        IncomingNotification incomingNotification = _gson.fromJson(notification.getData(), (new TypeToken<IncomingNotification>(){}).getType());

        switch (incomingNotification.getType()) {
            case SPLIT_UPDATE:
                incomingNotification = _gson.fromJson(notification.getData(), (new TypeToken<SplitChangeNotifiaction>(){}).getType());
                break;
            case SPLIT_KILL:
                incomingNotification = _gson.fromJson(notification.getData(), (new TypeToken<SplitKillNotification>(){}).getType());
                break;
            case SEGMENT_UPDATE:
                incomingNotification = _gson.fromJson(notification.getData(), (new TypeToken<SegmentChangeNotification>(){}).getType());
                break;
            default:
                throw new Exception("Incorrect Notification type");
        }

        incomingNotification.setChannel(notification.getChannel());

        return incomingNotification;
    }

    private IncomingNotification parseOccupancy(MessageNotification notification) {
        String channel = notification.getChannel().replace(StreamingConstants.occupancyPrefix, "");

        if (notification.getData().contains("controlType")) {
            ControlNotification controlNotification = _gson.fromJson(notification.getData(), (new TypeToken<ControlNotification>(){}).getType());
            controlNotification.setType(NotificationType.CONTROL);
            controlNotification.setChannel(channel);

            return controlNotification;
        }

        OccupancyNotification occupancyNotification = _gson.fromJson(notification.getData(), (new TypeToken<OccupancyNotification>(){}).getType());
        occupancyNotification.setType(NotificationType.OCCUPANCY);
        occupancyNotification.setChannel(channel);

        return occupancyNotification;
    }

    private ErrorNotification parseError(String payload) throws Exception {
        ErrorNotification messageError = _gson.fromJson(payload, (new TypeToken<ErrorNotification>(){}).getType());

        if (messageError.getMessage() == null || messageError.getStatusCode() == null) throw new Exception("Incorrect notification format.");

        messageError.setType(NotificationType.ERROR);

        return  messageError;
    }
}
