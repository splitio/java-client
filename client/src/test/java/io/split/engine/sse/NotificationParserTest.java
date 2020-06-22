package io.split.engine.sse;

import io.split.engine.sse.dtos.*;
import io.split.engine.sse.exceptions.EventParsingException;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotificationParserTest {
    private static String eventMessage = "message";
    private static String eventError = "error";

    @Test
    public void parseSplitUpdateShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1592590435115}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);
        assertEquals(NotificationType.SPLIT_UPDATE, result.getType());
        assertEquals("xxxx_xxxx_splits", result.getChannel());
        assertEquals(1592590435115L, ((SplitChangeNotifiaction) result).getChangeNumber());
    }

    @Test
    public void parseSplitKillShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591081575,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_KILL\\\",\\\"changeNumber\\\":1592591080818,\\\"defaultTreatment\\\":\\\"off\\\",\\\"splitName\\\":\\\"test-split\\\"}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);
        assertEquals(NotificationType.SPLIT_KILL, result.getType());
        assertEquals("xxxx_xxxx_splits", result.getChannel());
        assertEquals(1592591080818L, ((SplitKillNotification) result).getChangeNumber());
        assertEquals("test-split", ((SplitKillNotification) result).getSplitName());
        assertEquals("off", ((SplitKillNotification) result).getDefaultTreatment());
    }

    @Test
    public void parseSegmentUpdateShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591696052,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_segments\",\"data\":\"{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1592591695856,\\\"segmentName\\\":\\\"test-segment\\\"}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);
        assertEquals(NotificationType.SEGMENT_UPDATE, result.getType());
        assertEquals("xxxx_xxxx_segments", result.getChannel());
        assertEquals(1592591695856L, ((SegmentChangeNotification) result).getChangeNumber());
        assertEquals("test-segment", ((SegmentChangeNotification) result).getSegmentName());
    }

    @Test
    public void parseErrorShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.io/error/40142\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventError, payload);
        assertEquals(NotificationType.ERROR, result.getType());
        assertEquals("Token expired", ((ErrorNotification) result).getMessage());
        assertEquals("401", ((ErrorNotification) result).getStatusCode());
        assertNull(result.getChannel());
    }

    @Test
    public void parseIncorrectFormatShouldReturnException() {
        String payload = "{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591696052,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_segments\",\"data\":\"{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1592591695856,\\\"segmentName\\\":\\\"test-segment\\\"}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        try {
            IncomingNotification result = notificationParser.parse(eventError, payload);
        } catch (EventParsingException ex) {
            assertEquals(payload, ex.getPayload());
        }

        try {
            payload = "{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.io/error/40142\"}";
            notificationParser.parse(eventMessage, payload);
        }
        catch (EventParsingException ex) {
            assertEquals(payload, ex.getPayload());
        }
    }

    @Test
    public void parseIncorrectNotificationTypeShouldReturnException() {
        String payload = "{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591696052,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_segments\",\"data\":\"{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1592591695856,\\\"segmentName\\\":\\\"test-segment\\\"}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        try {
            notificationParser.parse(eventError, payload);
        } catch (EventParsingException ex) {
            assertEquals(payload, ex.getPayload());
        }

        try {
            payload = payload = "{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591696052,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_segments\",\"data\":\"{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1592591695856,\\\"segmentName\\\":\\\"test-segment\\\"}\"}";
            notificationParser.parse(eventMessage, payload);
        } catch (EventParsingException ex) {
            assertEquals(payload, ex.getPayload());
        }
    }

    @Test
    public void parseOccupancyControlPriShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"222\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":2}}\",\"name\":\"[meta]occupancy\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);

        assertEquals(NotificationType.OCCUPANCY, result.getType());
        assertEquals("control_pri", result.getChannel());
        assertEquals(2, ((OccupancyNotification)result).getMetrics().getPublishers());
    }

    @Test
    public void parseOccupancyControlSecShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"111\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_sec\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":1}}\",\"name\":\"[meta]occupancy\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);

        assertEquals(NotificationType.OCCUPANCY, result.getType());
        assertEquals("control_sec", result.getChannel());
        assertEquals(1, ((OccupancyNotification)result).getMetrics().getPublishers());
    }

    @Test
    public void parseControlStreamingPausedShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"2222\",\"clientId\":\"3333\",\"timestamp\":1588254699236,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"type\\\":\\\"CONTROL\\\",\\\"controlType\\\":\\\"STREAMING_PAUSED\\\"}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);

        assertEquals(NotificationType.CONTROL, result.getType());
        assertEquals("control_pri", result.getChannel());
        assertEquals(ControlType.STREAMING_PAUSED, ((ControlNotification)result).getControlType());
    }

    @Test
    public void parseControlStreamingResumedShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"2222\",\"clientId\":\"3333\",\"timestamp\":1588254699236,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"type\\\":\\\"CONTROL\\\",\\\"controlType\\\":\\\"STREAMING_RESUMED\\\"}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);

        assertEquals(NotificationType.CONTROL, result.getType());
        assertEquals("control_pri", result.getChannel());
        assertEquals(ControlType.STREAMING_RESUMED, ((ControlNotification)result).getControlType());
    }

    @Test
    public void parseControlStreamingDisabledShouldReturnParsedEvent() throws EventParsingException {
        String payload = "{\"id\":\"2222\",\"clientId\":\"3333\",\"timestamp\":1588254699236,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"type\\\":\\\"CONTROL\\\",\\\"controlType\\\":\\\"STREAMING_DISABLED\\\"}\"}";

        NotificationParser notificationParser = new NotificationParserImp();
        IncomingNotification result = notificationParser.parse(eventMessage, payload);

        assertEquals(NotificationType.CONTROL, result.getType());
        assertEquals("control_pri", result.getChannel());
        assertEquals(ControlType.STREAMING_DISABLED, ((ControlNotification)result).getControlType());
    }
}
