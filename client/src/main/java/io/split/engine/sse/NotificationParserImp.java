package io.split.engine.sse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.split.engine.sse.dtos.*;

public class NotificationParserImp implements NotificationParser {
    private Gson _gson;

    public NotificationParserImp(){
        _gson = new Gson();
    }

    @Override
    public IncomingNotification parse(String type, String payload) {
        if (type.equals(StreamingConstants.eventMessage)) {
            MessageNotification messageNotification = _gson.fromJson(payload, (new TypeToken<MessageNotification>(){}).getType());

            if (messageNotification.getChannel() == null || messageNotification.getData() == null) return null;

            if (messageNotification.getChannel().contains(StreamingConstants.occupancyPrefix)) {
                return parseOccupancy(messageNotification);
            }

            return parseNotification(messageNotification);
        } else if (type.equals(StreamingConstants.eventError)) {
            return parseError(payload);
        }

        return null;
    }

    private IncomingNotification parseNotification(MessageNotification notification) {
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
                return null;
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

    private ErrorNotification parseError(String payload) {
        ErrorNotification messageError = _gson.fromJson(payload, (new TypeToken<ErrorNotification>(){}).getType());

        if (messageError.getMessage() == null || messageError.getStatusCode() == null) return null;

        messageError.setType(NotificationType.ERROR);

        return  messageError;
    }
}
