package io.split.engine.sse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.split.engine.sse.dtos.*;
import io.split.engine.sse.exceptions.EventParsingException;

import java.lang.reflect.Type;
import java.util.Map;

public class NotificationParserImp implements NotificationParser {
    private static final String OCCUPANCY_PREFIX = "[?occupancy=metrics.publishers]";
    private static final Type MAP_STRING_OBJECT_TYPE_TOKEN = new TypeToken<Map<String, Object>>(){}.getType();
    private static final String  NOTIFICATION_TYPE_FIELD = "type";

    private final Gson _gson;

    public NotificationParserImp(Gson gson) {
        _gson = gson;
    }

    @Override
    public IncomingNotification parseMessage(String payload) throws EventParsingException {
        try {
            RawMessageNotification rawMessageNotification = _gson.fromJson(payload, RawMessageNotification.class);

            if (rawMessageNotification.getChannel().contains(OCCUPANCY_PREFIX)) {
                return parseControlChannelMessage(rawMessageNotification);
            }

            return parseNotification(rawMessageNotification);
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

    private IncomingNotification parseNotification(RawMessageNotification rawMessageNotification) throws Exception {
        Map<String, Object> data = _gson.fromJson(rawMessageNotification.getData(), MAP_STRING_OBJECT_TYPE_TOKEN);
        IncomingNotification.Type type = IncomingNotification.Type.valueOf((String) data.get(NOTIFICATION_TYPE_FIELD));

        switch (type) {
            case SPLIT_UPDATE:
                return buildSplitChangeNotification(rawMessageNotification.getChannel(), data);
            case SPLIT_KILL:
                return buildSplitKillNotification(rawMessageNotification.getChannel(), data);
            case SEGMENT_UPDATE:
                return buildSegmentChangeNotification(rawMessageNotification.getChannel(), data);
            default:
                throw new Exception("Wrong Notification type");
        }
    }

    private IncomingNotification parseControlChannelMessage(RawMessageNotification rawMessageNotification) {
        Map<String, Object> data = _gson.fromJson(rawMessageNotification.getData(), MAP_STRING_OBJECT_TYPE_TOKEN);
        Object controlType = data.get("controlType");
        String channel =  rawMessageNotification.getChannel().replace(OCCUPANCY_PREFIX, "");

        if (controlType != null) {
            return buildControlNotification(channel, ControlType.valueOf((String) controlType));
        }

        return buildOccupancyNotification(channel, data);
    }

    private SplitChangeNotification buildSplitChangeNotification(String channel, Map<String, Object> data) {
        return new SplitChangeNotification(channel, Double.valueOf((double)data.get("changeNumber")).longValue());
    }

    private SplitKillNotification buildSplitKillNotification(String channel, Map<String, Object> data) {
        return new SplitKillNotification(channel, Double.valueOf((double)data.get("changeNumber")).longValue(), (String) data.get("defaultTreatment"), (String) data.get("splitName"));
    }

    private SegmentChangeNotification buildSegmentChangeNotification(String channel, Map<String, Object> data) {
        return new SegmentChangeNotification(channel, Double.valueOf((double)data.get("changeNumber")).longValue(), (String) data.get("segmentName"));
    }

    private ControlNotification buildControlNotification(String channel, ControlType controlType) {
        return new ControlNotification(channel, controlType);
    }

    private OccupancyNotification buildOccupancyNotification(String channel, Map<String, Object> data) {
        Map<String, Object> metrics = (Map<String, Object>) data.get("metrics");
        int publishers = Double.valueOf((double)metrics.get("publishers")).intValue();

        return new OccupancyNotification(channel, publishers);
    }
}
