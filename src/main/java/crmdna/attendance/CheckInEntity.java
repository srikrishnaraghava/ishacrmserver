package crmdna.attendance;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Serialize;

import java.util.ArrayList;
import java.util.List;

@Entity
public class CheckInEntity {
    @Id
    String key; // programId_memberId
    @Index
    long programId;
    @Index
    long memberId;

    @Serialize
    List<CheckInRecord> checkInRecords = new ArrayList<>();

    public CheckInProp toProp() {
        CheckInProp checkInProp = new CheckInProp();
        checkInProp.key = key;
        checkInProp.programId = programId;
        checkInProp.memberId = memberId;
        checkInProp.checkInRecords = checkInRecords;

        return checkInProp;
    }
}
