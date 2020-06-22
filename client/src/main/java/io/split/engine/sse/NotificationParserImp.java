package io.split.engine.sse;

import com.google.gson.Gson;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.MessageNotification;
import io.split.engine.sse.dtos.SplitChangeNotifiaction;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.engine.sse.dtos.SegmentChangeNotification;
import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.OccupancyNotification;
import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.Notification;
import io.split.engine.sse.exceptions.EventParsingException;

public class NotificationParserImp implements NotificationParser {
    public static String OCCUPANCY_PREFIX = "[?occupancy=metrics.publishers]";

    private static Gson _gson = new Gson();

    @Override
    public IncomingNotification parse(String type, String payload) throws EventParsingException {
        try {
            switch (type) {
                case "message":
                    MessageNotification messageNotification = _gson.fromJson(payload, MessageNotification.class);

                    if (messageNotification.getChannel().contains(OCCUPANCY_PREFIX)) {
                        return parseOccupancy(messageNotification);
                    }

                    return parseNotification(messageNotification);
                case "error":
                    return parseError(payload);
                default:
                    throw new Exception("Incorrect Notification type.");
            }
        } catch (Exception ex) {
            throw new EventParsingException(ex, payload);
        }
    }

    private IncomingNotification parseNotification(MessageNotification notification) throws Exception {
        IncomingNotification incomingNotification = _gson.fromJson(notification.getData(), IncomingNotification.class);

        switch (incomingNotification.getType()) {
            case SPLIT_UPDATE:
                incomingNotification = _gson.fromJson(notification.getData(), SplitChangeNotifiaction.class);
                break;
            case SPLIT_KILL:
                incomingNotification = _gson.fromJson(notification.getData(), SplitKillNotification.class);
                break;
            case SEGMENT_UPDATE:
                incomingNotification = _gson.fromJson(notification.getData(), SegmentChangeNotification.class);
                break;
            default:
                throw new Exception("Incorrect Notification type");
        }

        incomingNotification.setChannel(notification.getChannel());

        return incomingNotification;
    }

    private IncomingNotification parseOccupancy(MessageNotification notification) {
        String channel = notification.getChannel().replace(OCCUPANCY_PREFIX, "");

        if (notification.getData().contains("controlType")) {
            ControlNotification controlNotification = _gson.fromJson(notification.getData(), ControlNotification.class);
            controlNotification.setType(Notification.Type.CONTROL);
            controlNotification.setChannel(channel);

            return controlNotification;
        }

        OccupancyNotification occupancyNotification = _gson.fromJson(notification.getData(), OccupancyNotification.class);
        occupancyNotification.setType(Notification.Type.OCCUPANCY);
        occupancyNotification.setChannel(channel);

        return occupancyNotification;
    }

    private ErrorNotification parseError(String payload) throws Exception {
        ErrorNotification messageError = _gson.fromJson(payload, ErrorNotification.class);

        if (messageError.getMessage() == null || messageError.getStatusCode() == null) throw new Exception("Incorrect notification format.");

        messageError.setType(Notification.Type.ERROR);

        return  messageError;
    }
}
