package crmdna.attendance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckInProp {
    public String key; // programId_memberId
    public long programId;
    public long memberId;

    List<CheckInRecord> checkInRecords = new ArrayList<>();

    CheckInRecord getCheckInRecord(int sessionDateYYYYMMDD) {
        for (int i = checkInRecords.size() - 1; i >= 0; i--) {
            CheckInRecord checkInOrOut = checkInRecords.get(i);
            if (checkInOrOut.sessionDateYYYYMMDD == sessionDateYYYYMMDD)
                return checkInOrOut;
        }

        return null;
    }

    int getNumCheckins() {
        Map<Integer, Boolean> map = new HashMap<>();

        for (CheckInRecord checkInRecord : checkInRecords) {
            if (checkInRecord.isCheckin) {
                map.put(checkInRecord.sessionDateYYYYMMDD, true);
            }
        }

        int numCheckins = 0;
        for (Integer date : map.keySet()) {
            if (map.get(date) == true)
                numCheckins++;
        }

        return numCheckins;
    }
}
