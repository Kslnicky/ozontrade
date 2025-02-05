package me.yukitale.yellowexchange.utils;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@UtilityClass
public class StringUtil {

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private final SimpleDateFormat DATE_FORMAT_WITHOUT_SECONDS = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private final SimpleDateFormat DATE_FORMAT_WITHOUT_YEAR = new SimpleDateFormat("MM/dd HH:mm");

    public String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public String formatDateWithoutYears(Date date) {
        return DATE_FORMAT_WITHOUT_YEAR.format(date);
    }

    public String formatDateWithoutSeconds(Date date) {
        return DATE_FORMAT_WITHOUT_SECONDS.format(date);
    }

    public String formatDateToTimeAgo(long time) {
        long diff = (System.currentTimeMillis() - time) / 1000L;
        if (diff < 60) {
            return diff + " s. ago";
        } else if (diff > 86400) {
            return formatDate(new Date(time));
        } else if (diff > 3600) {
            return diff / 3600 + "h. ago";
        } else {
            return diff / 60 + " min. ago";
        }
    }

    public static String formatDecimal(long number) {
        String numStr = Long.toString(number);
        StringBuilder formattedNumber = new StringBuilder();
        int length = numStr.length();

        for (int i = 0; i < length; i += 3) {
            if (i != 0) {
                formattedNumber.append(",");
            }
            int end = Math.min(i + 3, length);
            formattedNumber.append(numStr, i, end);
        }

        if (!formattedNumber.toString().contains(".")) {
            formattedNumber.append(".00");
        }

        return formattedNumber.toString();
    }

}
