package crmdna.attendance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Attendance {
    public static List<AttendanceProp> getQSMatchesDummy(long programId,
                                                         String searchStr, String login) {

        List<AttendanceProp> attendanceProps = new ArrayList<>();

        AttendanceProp attendanceProp = new AttendanceProp();
        attendanceProp.name = "Sharmila Napa";
        attendanceProp.email = "sharmila.napa@gmail.com";
        attendanceProp.mobilePhone = "+6581248184";
        attendanceProp.homePhone = "+6531612535";
        attendanceProp.centers.add("Singapore");
        attendanceProp.practices.add("Isha Kriya");
        attendanceProp.practices.add("Shambhavi");
        attendanceProp.practices.add("Shoonya");
        attendanceProp.practices.add("BSP");
        attendanceProp.practices.add("Samyama");

        Collections.sort(attendanceProp.practices);
        Collections.sort(attendanceProp.centers);
        attendanceProp.sessionWiseAttendance.add(true);
        attendanceProp.sessionWiseAttendance.add(false);
        attendanceProps.add(attendanceProp);

        attendanceProp = new AttendanceProp();
        attendanceProp.name = "Malliga K";
        attendanceProp.email = "malligamkm@yahoo.com.sg";
        attendanceProp.mobilePhone = "+6591144676";
        attendanceProp.centers.add("Singapore");
        attendanceProp.practices.add("Shambhavi");
        attendanceProp.practices.add("Shoonya");

        Collections.sort(attendanceProp.practices);
        Collections.sort(attendanceProp.centers);
        attendanceProp.sessionWiseAttendance.add(true);
        attendanceProp.sessionWiseAttendance.add(false);
        attendanceProps.add(attendanceProp);

        attendanceProp = new AttendanceProp();
        attendanceProp.name = "Sasikumar Thanabal";
        attendanceProp.email = "sasikumar.imfs@gmail.com";
        attendanceProp.centers.add("Singapore");
        attendanceProp.practices.add("Shoonya");

        Collections.sort(attendanceProp.practices);
        Collections.sort(attendanceProp.centers);
        attendanceProp.sessionWiseAttendance.add(false);
        attendanceProp.sessionWiseAttendance.add(false);
        attendanceProps.add(attendanceProp);

        Collections.sort(attendanceProps);

        return attendanceProps;
    }

    public static AttendanceReportProp getReportDummy(long programId,
                                                      String login) {
        AttendanceReportProp attendanceReportProp = new AttendanceReportProp();

        attendanceReportProp.programType = "Surya Kriya";
        attendanceReportProp.venue = "Grassroots Club";
        attendanceReportProp.center = "Singapore";
        attendanceReportProp.startDateddMMMyy = "8-Mar-14";
        attendanceReportProp.endDateddMMMyy = "9-Mar-14";
        attendanceReportProp.numSessions = 2;
        attendanceReportProp.attendanceProps = getQSMatchesDummy(programId,
                "dummy", login);

        return attendanceReportProp;
    }

    public static class AttendanceProp implements Comparable<AttendanceProp> {
        public String name;
        public String email;
        public String mobilePhone;
        public String officePhone;
        public String homePhone;
        public List<String> centers = new ArrayList<>();
        public List<String> practices = new ArrayList<>();
        public List<Boolean> sessionWiseAttendance = new ArrayList<>();

        @Override
        public int compareTo(AttendanceProp o) {
            return this.name.compareTo(o.name);
        }
    }

    public static class AttendanceReportProp {
        public String programType;
        public String venue;
        public String center;
        public String venueFullAddress;
        public String startDateddMMMyy;
        public String endDateddMMMyy;
        public int numSessions;
        public List<AttendanceProp> attendanceProps = new ArrayList<>();
    }
}
