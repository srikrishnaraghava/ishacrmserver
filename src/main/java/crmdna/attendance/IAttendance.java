package crmdna.attendance;

import java.util.List;

public interface IAttendance {
    public int checkin(long memberId, long programId, int sessionDateYYYYMMDD,
                       int batchNo, String login);

    public int checkout(long memberId, long programId, int sessionDateYYYYMMDD,
                        String login);

    public int getNumCheckins(long programId, int sessionDateYYYYMMDD,
                              int batchNo);

    public List<CheckInMemberProp> getMembersForCheckIn(String searchStr,
                                                        long programId, int sessionDateYYYYMMDD, int maxResultSize,
                                                        String login);
}
