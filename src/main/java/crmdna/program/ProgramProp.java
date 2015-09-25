package crmdna.program;

import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.group.Group.GroupProp;
import crmdna.programtype.ProgramTypeProp;
import crmdna.registration.RegistrationSummaryProp;
import crmdna.teacher.Teacher.TeacherProp;
import crmdna.venue.Venue.VenueProp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ProgramProp implements Comparable<ProgramProp> {
    public long programId;
    public ProgramTypeProp programTypeProp;
    public GroupProp groupProp;
    public VenueProp venueProp;
    public TeacherProp teacherProp;
    public int startYYYYMMDD;
    public int endYYYYMMDD;
    public int numBatches;
    public int maxParticipants;
    public boolean isRegistrationLimitReached;
    public boolean disabled;
    public String description;
    public RegistrationSummaryProp regSummary;

    public double fee;
    public Currency ccy;

    public List<String> batch1SessionTimings;
    public List<String> batch2SessionTimings;
    public List<String> batch3SessionTimings;
    public List<String> batch4SessionTimings;
    public List<String> batch5SessionTimings;

    public String specialInstruction;

    // dependent
    public String name;
    public String shortName;

    public void populateName() {
        name =
                programTypeProp.displayName + " "
                        + DateUtils.getDurationAsString(startYYYYMMDD, endYYYYMMDD) + " @ "
                        + venueProp.displayName;
    }

    @Override
    public int compareTo(ProgramProp arg0) {
        // sort order is: -startYYYYMMDD, +programtypename, +group, +venue

        if (startYYYYMMDD != arg0.startYYYYMMDD) {
            Long start = new Long(startYYYYMMDD);
            Long arg0Start = new Long(arg0.startYYYYMMDD);
            return arg0Start.compareTo(start);
        }

        if (!programTypeProp.name.equals(arg0.programTypeProp.name))
            return programTypeProp.name.compareTo(arg0.programTypeProp.name);

        if (!groupProp.name.equals(arg0.groupProp.name))
            return groupProp.name.compareTo(arg0.groupProp.name);

        return venueProp.name.compareTo(arg0.venueProp.name);
    }

    public List<SessionProp> getSessions(int dateYYYYMMDD) {
        List<SessionProp> sessionProps = new ArrayList<>();

        for (int batchIndex = 0; batchIndex < numBatches; batchIndex++) {
            SessionProp sessionProp = new SessionProp();
            sessionProp.dateYYYYMMDD = dateYYYYMMDD;
            sessionProp.programId = programId;
            sessionProp.batchNo = batchIndex + 1;
            sessionProp.programType = programTypeProp.displayName;
            sessionProp.center = groupProp.displayName;
            sessionProp.venue = venueProp.displayName;

            sessionProp.populateTitle(numBatches);

            sessionProps.add(sessionProp);
        }

        return sessionProps;
    }

    public int getNumSessions()  {

        return DateUtils.getNumDays(startYYYYMMDD, endYYYYMMDD) + 1;
    }

    public void ensureValidSessionDate(int sessionDateYYYYMMDD) {
        DateUtils.ensureFormatYYYYMMDD(sessionDateYYYYMMDD);

        if ((sessionDateYYYYMMDD < startYYYYMMDD) || (sessionDateYYYYMMDD > endYYYYMMDD))
            Utils.throwIncorrectSpecException("Session date [" + sessionDateYYYYMMDD
                    + "] should be between start [" + startYYYYMMDD + "] and end date [" + endYYYYMMDD
                    + "] for Program [" + name + "]");
    }

    public Map<String, Object> asMap() {
        Map<String, Object> treeMap = new TreeMap<>();

        treeMap.put("programId", new Long(programId));
        treeMap.put("programType", programTypeProp.displayName);
        treeMap.put("startDateYYYYMMDD", startYYYYMMDD + "");
        treeMap.put("endDateYYYYMMDD", endYYYYMMDD + "");
        treeMap.put("venue", venueProp.displayName);
        treeMap.put("venueFullAddress", venueProp.address);
        treeMap.put("groupName", groupProp.displayName);

        return treeMap;
    }

    public String getDetailsAsHtml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<b>Program Details</b><br>");
        builder.append("<i>Program Type:</i> " + programTypeProp.displayName + "<br>");
        builder.append("<i>Center:</i> " + groupProp.displayName + "<br>");
        builder.append("<i>Date</i>: " + DateUtils.getDurationAsString(startYYYYMMDD, endYYYYMMDD)
                + "<br>");
        builder.append("<i>Venue: </i>" + venueProp.displayName + "<br>");
        builder.append("<i>Venue Full Address: </i>" + venueProp.displayName + "<br>");
        builder.append("<i>Teacher: </i>" + teacherProp.email + "<br><br>");

        return builder.toString();
    }

    public String getNameWOVenue() {
        name =
                programTypeProp.displayName + " "
                        + DateUtils.getDurationAsString(startYYYYMMDD, endYYYYMMDD) + " @ "
                        + groupProp.displayName;

        return name;
    }

    public String getDetailedName() {
        return programTypeProp.displayName + " on "
            + DateUtils.getDurationAsString(startYYYYMMDD, endYYYYMMDD) + " @ "
            + venueProp.shortName;
    }
}
