package crmdna.attendance;

import crmdna.client.isha.IshaUtils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.program.Program;
import crmdna.program.ProgramProp;

import java.util.List;

public class AttendanceIshaImpl extends AttendanceDefaultImpl implements IAttendance {

    AttendanceIshaImpl(String client) {
        super(client);
    }

    @Override
    public List<CheckInMemberProp> getMembersForCheckIn(String searchStr, long programId,
                                                        int sessionDateYYYYMMDD, int maxResultSize, String login) {

        ProgramProp programProp = Program.safeGet(client, programId).toProp("isha");

        if (!IshaUtils.isSathsang(programProp.name)) {
            throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                    "Check in not available yet for [" + programProp.programTypeProp.displayName + "]");
        }

        List<CheckInMemberProp> checkInMemberProps =
                super.getMembersForCheckIn(searchStr, programId, sessionDateYYYYMMDD, maxResultSize, login);

        for (CheckInMemberProp checkInMemberProp : checkInMemberProps) {
            if (!IshaUtils.isMeditator(checkInMemberProp.practiceIds)) {
                checkInMemberProp.allow = false;
                checkInMemberProp.notAllowingReason = "Not meditator";
            }
        }

        return checkInMemberProps;
    }
}
