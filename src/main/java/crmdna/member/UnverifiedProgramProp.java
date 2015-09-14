package crmdna.member;

import crmdna.common.DateUtils;
import crmdna.common.DateUtils.Month;

import java.io.Serializable;

public class UnverifiedProgramProp implements
        Comparable<UnverifiedProgramProp>, Serializable {

    private static final long serialVersionUID = 1L;

    public int unverifiedProgramId; // starts with 1 for every member and keeps increasing
    public long programTypeId;
    public Month month;
    public int year;
    String teacher;
    String city;
    String country;

    @Override
    public int compareTo(UnverifiedProgramProp arg0) {
        // sorted in descending order of time

        Integer yyyymm = year * 100 + DateUtils.getZeroBasedMonthIndex(month)
                + 1;
        Integer arg0yyyymm = arg0.year * 100
                + DateUtils.getZeroBasedMonthIndex(arg0.month) + 1;

        return arg0yyyymm.compareTo(yyyymm);
    }
}
