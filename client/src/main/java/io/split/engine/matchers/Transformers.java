package io.split.engine.matchers;

import com.google.common.collect.Sets;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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

    public static Boolean asBoolean(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }

        if (obj instanceof String) {
            if (Sets.newHashSet("true", "false").contains(((String) obj).toLowerCase())) {
                return Boolean.parseBoolean((String) obj);
            }
        }

        return null;
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


    public static Set<String> toSetOfStrings(Collection key) {
        Set<String> result = new HashSet<String>(key.size());
        for (Object o : key) {
            result.add(o.toString());
        }
        return result;
    }

}
