package crmdna.common;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.text.SimpleDateFormat;
import java.util.*;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;

public class DateUtils {

    public static void ensureDateNotInFuture(Month month, int year) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        ensure(year != 0, "year is 0");
        ensure(year <= currentYear, "year cannot be greater than current year");

        ensureNotNull(month, "month is null");

        if (year == currentYear) {
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            ensure(currentMonth >= getZeroBasedMonthIndex(month), "future date not allowed");
        }
    }

    public static Month getMonthEnum(int yyyymmdd) {
        ensureFormatYYYYMMDD(yyyymmdd);

        int yyyymm = yyyymmdd / 100;
        int mm = yyyymm % 100;

        switch (mm) {
            case 1:
                return Month.JAN;
            case 2:
                return Month.FEB;
            case 3:
                return Month.MAR;
            case 4:
                return Month.APR;
            case 5:
                return Month.MAY;
            case 6:
                return Month.JUN;
            case 7:
                return Month.JUL;
            case 8:
                return Month.AUG;
            case 9:
                return Month.SEP;
            case 10:
                return Month.OCT;
            case 11:
                return Month.NOV;
            case 12:
                return Month.DEC;
            default:
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid month number [" + mm + "]");
        }
    }

    public static int getZeroBasedMonthIndex(Month month) {
        switch (month) {
            case JAN:
                return 0;
            case FEB:
                return 1;
            case MAR:
                return 2;
            case APR:
                return 3;
            case MAY:
                return 4;
            case JUN:
                return 5;
            case JUL:
                return 6;
            case AUG:
                return 7;
            case SEP:
                return 8;
            case OCT:
                return 9;
            case NOV:
                return 10;
            case DEC:
                return 11;
        }

        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "invalid month [" + month + "]");
    }

    public static long getMilliSecondsFromDateRange(FutureDateRange dateRange) {
        ensureNotNull(dateRange);

        final long NUM_MS_PER_DAY = 86400 * 1000;

        if (dateRange == FutureDateRange.NEXT_15_DAYS)
            return NUM_MS_PER_DAY * 15;

        if (dateRange == FutureDateRange.NEXT_30_DAYS)
            return NUM_MS_PER_DAY * 30;

        if (dateRange == FutureDateRange.NEXT_60_DAYS)
            return NUM_MS_PER_DAY * 60;

        if (dateRange == FutureDateRange.NEXT_90_DAYS)
            return NUM_MS_PER_DAY * 90;

        throw new APIException("Cannot get milli seconds from data range [" + dateRange + "]")
                .status(Status.ERROR_RESOURCE_INCORRECT);
    }

    public static long getMilliSecondsFromDateRange(DateRange dateRange) {
        ensureNotNull(dateRange);

        final long NUM_MS_PER_DAY = 86400 * 1000;

        if (dateRange == DateRange.LAST_24_HOURS)
            return NUM_MS_PER_DAY;

        if (dateRange == DateRange.LAST_48_HOURS)
            return NUM_MS_PER_DAY * 2;

        if (dateRange == DateRange.LAST_72_HOURS)
            return NUM_MS_PER_DAY * 3;

        if (dateRange == DateRange.LAST_7_DAYS)
            return NUM_MS_PER_DAY * 7;

        if (dateRange == DateRange.LAST_15_DAYS)
            return NUM_MS_PER_DAY * 15;

        if (dateRange == DateRange.LAST_30_DAYS)
            return NUM_MS_PER_DAY * 30;

        if (dateRange == DateRange.LAST_90_DAYS)
            return NUM_MS_PER_DAY * 90;

        if (dateRange == DateRange.LAST_180_DAYS)
            return NUM_MS_PER_DAY * 180;

        if (dateRange == DateRange.LAST_365_DAYS)
            return NUM_MS_PER_DAY * 365;

        throw new APIException("Cannot get milli seconds from data range [" + dateRange + "]")
                .status(Status.ERROR_RESOURCE_INCORRECT);

    }

    public static int toYYYYMMDD(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        int yyyymmdd = day + month * 100 + year * 10000;
        return yyyymmdd;
    }

    public static Date toDate(int yyyymmdd) {

        if (!isFormatInYYYYMMDD(yyyymmdd)) {
            Utils.throwIncorrectSpecException("[" + yyyymmdd + "] is not in yyymmdd format");
        }

        String s = yyyymmdd + "";

        int year = Integer.parseInt(s.substring(0, 4));
        int month = Integer.parseInt(s.substring(4, 6));
        int day = Integer.parseInt(s.substring(6, 8));

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);

        return calendar.getTime();
    }

    public static boolean isFormatInYYYYMMDD(int date) {
        String s = "" + date;

        if (s.length() != 8)
            return false;

        int year = Integer.parseInt(s.substring(0, 4));
        int month = Integer.parseInt(s.substring(4, 6));
        int day = Integer.parseInt(s.substring(6, 8));

        if ((year < 1970) || (year > 2050))
            return false;

        if ((month < 1) || (month > 12))
            return false;

        if ((day < 1) || (day > 31))
            return false;

        if (month == 2) {
            if (day > 29)
                return false;

            if ((day == 29) && (year % 4 != 0))
                return false;
        }

        if ((month == 4) || (month == 6) || (month == 9) || (month == 11))
            if (day == 31)
                return false;

        return true;
    }

    public static boolean isFormatInYYYYMM(int date) {
        String s = "" + date + "01";

        if (s.length() != 8)
            return false;

        int year = Integer.parseInt(s.substring(0, 4));
        int month = Integer.parseInt(s.substring(4, 6));
        int day = Integer.parseInt(s.substring(6, 8));

        if ((year < 1970) || (year > 2050))
            return false;

        if ((month < 1) || (month > 12))
            return false;

        if ((day < 1) || (day > 31))
            return false;

        if (month == 2) {
            if (day > 29)
                return false;

            if ((day == 29) && (year % 4 != 0))
                return false;
        }

        if ((month == 4) || (month == 6) || (month == 9) || (month == 11))
            if (day == 31)
                return false;

        return true;
    }

    private static Map<String, String> getMMMtoMMMap() {
        Map<String, String> map = new HashMap<>();

        map.put("jan", "01");
        map.put("feb", "02");
        map.put("mar", "03");
        map.put("apr", "04");
        map.put("may", "05");
        map.put("jun", "06");
        map.put("jul", "07");
        map.put("aug", "08");
        map.put("sep", "09");
        map.put("oct", "10");
        map.put("nov", "11");
        map.put("dec", "12");

        return map;
    }

    public static boolean isFormatInMMMYYYY(String s) {

        if (s == null)
            return false;

        s = Utils.removeSpaceUnderscoreBracketAndHyphen(s).toLowerCase();

        if (s.length() != 7)
            return false;

        String mmm = s.substring(0, 3);
        String yyyy = s.substring(3, 7);

        Map<String, String> mmmTommMap = getMMMtoMMMap();
        if (mmmTommMap.containsKey(mmm))
            return false;

        if (!Utils.canParseAsLong(yyyy))
            return false;

        return true;
    }

    public static String toDDMMM(int dateYYYYMMDD) {
        ensureFormatYYYYMMDD(dateYYYYMMDD);

        String s = "" + dateYYYYMMDD;

        int month = Integer.parseInt(s.substring(4, 6));
        int day = Integer.parseInt(s.substring(6, 8));

        return day + " " + get3CharMonth(month);
    }

    public static String toYYYYMM(String mmmYYYY) {
        ensure(isFormatInMMMYYYY(mmmYYYY), "[" + mmmYYYY + "] is not in format MMMMYYYY");

        String mmm = mmmYYYY.substring(0, 3);
        String yyyy = mmmYYYY.substring(3, 7);
        ensure(mmm.length() == 3);
        ensure(yyyy.length() == 4);

        Map<String, String> map = getMMMtoMMMap();
        ensure(map.containsKey(mmm), "[" + mmm + "] is not a valid value for MMM");

        return yyyy + map.get(mmm);
    }

    public static String get3CharMonth(int month) {
        if (1 == month)
            return "Jan";

        if (2 == month)
            return "Feb";

        if (3 == month)
            return "Mar";

        if (4 == month)
            return "Apr";

        if (5 == month)
            return "May";

        if (6 == month)
            return "Jun";

        if (7 == month)
            return "Jul";

        if (8 == month)
            return "Aug";

        if (9 == month)
            return "Sep";

        if (10 == month)
            return "Oct";

        if (11 == month)
            return "Nov";

        if (12 == month)
            return "Dec";

        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "Invalid month [" + month + "]");
    }

    public static String getDurationAsString(int startYYYYMMDD, int endYYYYMMDD) {
        ensureFormatYYYYMMDD(startYYYYMMDD);
        ensureFormatYYYYMMDD(endYYYYMMDD);

        if (startYYYYMMDD > endYYYYMMDD)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Start date should not be greater than end date");

        if (startYYYYMMDD == endYYYYMMDD)
            return toDDMMMYY(startYYYYMMDD);

        String s = "" + startYYYYMMDD;
        int yearStart = Integer.parseInt(s.substring(0, 4));
        int monthStart = Integer.parseInt(s.substring(4, 6));
        int dayStart = Integer.parseInt(s.substring(6, 8));

        s = "" + endYYYYMMDD;
        int yearEnd = Integer.parseInt(s.substring(0, 4));
        int monthEnd = Integer.parseInt(s.substring(4, 6));

        if (yearStart == yearEnd) {
            if (monthStart == monthEnd) {
                return dayStart + " - " + toDDMMMYY(endYYYYMMDD);
            }

            return toDDMMM(startYYYYMMDD) + " - " + toDDMMMYY(endYYYYMMDD);
        }

        return toDDMMM(startYYYYMMDD) + " - " + toDDMMMYY(endYYYYMMDD);
    }

    public static String getDateDiff(long startTimeInSeconds, long endTimeInSeconds) {
        long diff = endTimeInSeconds - startTimeInSeconds;
        if (diff < 0)
            Utils.throwIncorrectSpecException("startTimeInSeconds greater than endTimeInSeconds");

        if (diff < 60)
            return diff + " seconds";

        if (diff < 3600) {
            long min = Math.round(diff / 60.0);
            return min + " minute(s)";
        }

        if (diff < 3600 * 24) {
            long hours = Math.round(diff / 3600.0);
            return hours + " hour(s)";
        }

        long days = Math.round(diff / 3600.0 / 24.0);
        return days + " day(s)";
    }

    public static String toDDMMMYY(int dateYYYYMMDD) {
        ensureFormatYYYYMMDD(dateYYYYMMDD);

        String s = "" + dateYYYYMMDD;

        int year = Integer.parseInt(s.substring(2, 4));
        int month = Integer.parseInt(s.substring(4, 6));
        int day = Integer.parseInt(s.substring(6, 8));

        return day + " " + get3CharMonth(month) + " '" + year;
    }

    public static void ensureFormatYYYYMMDD(int date) {

        boolean valid = isFormatInYYYYMMDD(date);
        if (!valid)
            throw new APIException("[" + date + "] is not a valid date in YYYYMMDD format.")
                    .status(Status.ERROR_RESOURCE_INCORRECT);

    }

    public static long getNanoSeconds(Date timestamp) {
        // returns time in nano seconds

        // system clocks are only accurate up to millisecond precision
        // get the time in ms and add a random no between 0 and 1 million to
        // convert into nanoseconds

        return timestamp.getTime() * 1000000 + new Random().nextInt(999999);
    }

    public static long getMicroSeconds(Date timestamp) {
        // returns time in nano seconds

        // system clocks are only accurate up to millisecond precision
        // get the time in ms and add a random no between 0 and 1 million to
        // convert into nanoseconds

        return timestamp.getTime() * 1000 + new Random().nextInt(999);
    }

    public static String toISOString(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(timestamp);
    }

    public static String toLongDateString(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        return sdf.format(timestamp);
    }

    public static int getNumDays(Date former, Date later) {
        final int MILLI_SECONDS_IN_A_DAY = 3600 * 24 * 1000;

        int numDays = (int) (later.getTime() - former.getTime()) / MILLI_SECONDS_IN_A_DAY;

        return numDays;
    }

    public enum Month {
        JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC
    }

    public enum DateRange {
        LAST_24_HOURS, LAST_48_HOURS, LAST_72_HOURS, LAST_7_DAYS, LAST_15_DAYS, LAST_30_DAYS, LAST_90_DAYS, LAST_180_DAYS, LAST_365_DAYS
    }

    public enum FutureDateRange {
        NEXT_15_DAYS, NEXT_30_DAYS, NEXT_60_DAYS, NEXT_90_DAYS
    }
}
