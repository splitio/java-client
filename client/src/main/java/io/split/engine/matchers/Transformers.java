package io.split.engine.matchers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by adilaijaz on 3/7/16.
 */
public class Transformers {

    private static TimeZone UTC = TimeZone.getTimeZone("UTC");

    public static Long asLong(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }

        if (obj instanceof Long) {
            return ((Long) obj).longValue();
        }

        return null;
    }

    public static Long asDate(Object obj) {
        Calendar c = toCalendar(obj);

        if (c == null) {
            return null;
        }

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTimeInMillis();
    }

    public static Long asDateHourMinute(Object obj) {

        Calendar c = toCalendar(obj);

        if (c == null) {
            return null;
        }

        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTimeInMillis();
    }

    private static Calendar toCalendar(Object obj) {
        Long millisecondsSinceEpoch = asLong(obj);

        if (millisecondsSinceEpoch == null) {
            return null;
        }

        Calendar c = Calendar.getInstance();
        c.setTimeZone(UTC);
        c.setTimeInMillis(millisecondsSinceEpoch.longValue());

        return c;
    }

}
