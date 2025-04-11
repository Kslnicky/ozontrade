package me.yukitale.yellowexchange.utils;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@UtilityClass
public class DateUtil {

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy, hh:mm a");

    public String getSimpleDate() {
        return DATE_FORMAT.format(new Date());
    }

    public Date getTodayStartDate() {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);

        return calendar.getTime();
    }

    public Date getWeekStartDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = 0;

        if (dayOfWeek > Calendar.MONDAY) {
            daysToSubtract = dayOfWeek - Calendar.MONDAY;
        } else if (dayOfWeek < Calendar.MONDAY) {
            daysToSubtract = dayOfWeek + (7 - Calendar.MONDAY);
        }

        calendar.add(Calendar.DAY_OF_WEEK, -daysToSubtract);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);

        return calendar.getTime();
    }

    public Date getMonthStartDate() {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(new Date());
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);

        return calendar.getTime();
    }

    public Date getYearStartDate() {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(new Date());
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);

        return calendar.getTime();
    }
}
