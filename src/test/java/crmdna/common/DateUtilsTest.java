package crmdna.common;

import crmdna.common.DateUtils.Month;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import org.junit.Test;

import java.util.Calendar;

import static crmdna.common.TestUtil.ensureResourceIncorrectException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateUtilsTest {
    @Test
    public void geDateDiffTest() {
        assertEquals("5 seconds", DateUtils.getDateDiff(0, 5));
        assertEquals("1 minute(s)", DateUtils.getDateDiff(0, 65));
        assertEquals("2 minute(s)", DateUtils.getDateDiff(0, 110));
        assertEquals("30 minute(s)", DateUtils.getDateDiff(0, 1820));
        assertEquals("1 hour(s)", DateUtils.getDateDiff(0, 3800));
        assertEquals("3 hour(s)", DateUtils.getDateDiff(0, 11000));

        assertEquals("1 day(s)", DateUtils.getDateDiff(0, 3600 * 24));
        assertEquals("2 day(s)", DateUtils.getDateDiff(0, 3600 * 24 * 2 + 1200));
    }

    @Test
    public void getDurationAsString() {
        int start;
        int end;

        // same start and end date
        start = 20130201;
        end = 20130201;
        assertEquals("1 Feb 13", DateUtils.getDurationAsString(start, end));

        // same month and year
        start = 20130201;
        end = 20130204;
        assertEquals("1 - 4 Feb 13", DateUtils.getDurationAsString(start, end));

        // different month same year
        start = 20130227;
        end = 20130304;
        assertEquals("27 Feb - 4 Mar 13", DateUtils.getDurationAsString(start, end));

        // different year
        start = 20131227;
        end = 20140104;
        assertEquals("27 Dec - 4 Jan 14", DateUtils.getDurationAsString(start, end));

        start = 2013123;
        end = 2013125;
        try {
            DateUtils.getDurationAsString(start, end);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void toYYYYMMDDTest() {
        Calendar calendar = Calendar.getInstance();

        // 1-jan-2014
        calendar.set(2014, 0, 1); // month goes from 0 to 11
        assertEquals(20140101, DateUtils.toYYYYMMDD(calendar.getTime()));

        // 31-dec-1989
        calendar.set(1989, 11, 31); // month goes from 0 to 11
        assertEquals(19891231, DateUtils.toYYYYMMDD(calendar.getTime()));

        // 29-feb-2004
        calendar.set(2004, 1, 29); // month goes from 0 to 11
        assertEquals(20040229, DateUtils.toYYYYMMDD(calendar.getTime()));
    }

    @Test
    public void isFormatInYYYYMMDD() {
        assertEquals(false, DateUtils.isFormatInYYYYMMDD(2));
        assertEquals(false, DateUtils.isFormatInYYYYMMDD(290394938));
        assertEquals(false, DateUtils.isFormatInYYYYMMDD(2013312));
        assertEquals(false, DateUtils.isFormatInYYYYMMDD(20130229));
        assertEquals(false, DateUtils.isFormatInYYYYMMDD(19000229));
        assertEquals(false, DateUtils.isFormatInYYYYMMDD(21000229));

        assertEquals(true, DateUtils.isFormatInYYYYMMDD(20140201));
        assertEquals(true, DateUtils.isFormatInYYYYMMDD(20140131));
        assertEquals(true, DateUtils.isFormatInYYYYMMDD(20131231));
        assertEquals(true, DateUtils.isFormatInYYYYMMDD(20140630));
    }

    @Test
    public void getNSTest() {
//    assertTrue(false);
    }

    @Test
    public void ensureDateNotInFutureTest() {
        DateUtils.ensureDateNotInFuture(Month.JUL, 2014);
        DateUtils.ensureDateNotInFuture(Month.AUG, 2013);

        ensureResourceIncorrectException(new ICode() {

            @Override
            public void run() {
                DateUtils.ensureDateNotInFuture(Month.AUG, 2018);
            }
        });

        ensureResourceIncorrectException(new ICode() {

            @Override
            public void run() {
                DateUtils.ensureDateNotInFuture(Month.JAN, 2020);
            }
        });
    }

    @Test
    public void getMonthEnumTest() {
        assertEquals(Month.JAN, DateUtils.getMonthEnum(20140101));
        assertEquals(Month.FEB, DateUtils.getMonthEnum(20140202));
        assertEquals(Month.FEB, DateUtils.getMonthEnum(19700202));
        assertEquals(Month.DEC, DateUtils.getMonthEnum(19701202));

        // exception if date in wrong format
        ensureResourceIncorrectException(new ICode() {

            @Override
            public void run() {
                DateUtils.getMonthEnum(100);
            }
        });

        // exception if month is invalid
        ensureResourceIncorrectException(new ICode() {

            @Override
            public void run() {
                DateUtils.getMonthEnum(20141302);
            }
        });

    }
}
