package crmdna.program;

import crmdna.common.DateUtils;

public class SessionProp implements Comparable<SessionProp> {
    public int dateYYYYMMDD;
    public long programId;
    public String programType;
    public String venue;
    public String center;
    public int batchNo;
    public String title;

    @Override
    public int compareTo(SessionProp o) {
        return this.title.compareTo(o.title);
    }

    public void populateTitle(long numBatches) {
        String title = DateUtils.toDDMMM(dateYYYYMMDD) + " ";
        if (numBatches > 1)
            title += "B" + batchNo + " ";

        if (programType.length() > 11)
            title += programType.substring(0, 10) + "~" + " ";
        else {
            title += programType + " ";
        }

        if (venue.length() > 11)
            title += "@ " + venue.substring(0, 10) + "~";
        else
            title += "@ " + venue;

        this.title = title;
    }
}
