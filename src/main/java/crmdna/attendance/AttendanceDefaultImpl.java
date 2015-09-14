package crmdna.attendance;

import com.googlecode.objectify.Work;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.counter.Counter;
import crmdna.counter.Counter.CounterType;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static crmdna.common.OfyService.ofy;

public class AttendanceDefaultImpl implements IAttendance {

    protected String client;

    AttendanceDefaultImpl(String client) {
        Client.ensureValid(client);
        this.client = client;
    }

    @Override
    public int checkin(final long memberId, final long programId, final int sessionDateYYYYMMDD,
                       final int batchNo, final String login) {

        ensureValidCheckInInputs(memberId, programId, sessionDateYYYYMMDD, batchNo, login);

        final String key = getCheckInKey(programId, memberId);

        CheckInProp checkInProp = ofy(client).transact(new Work<CheckInProp>() {

            @Override
            public CheckInProp run() {
                CheckInEntity checkInEntity = ofy(client).load().type(CheckInEntity.class).id(key).now();

                if (null == checkInEntity) {
                    checkInEntity = new CheckInEntity();
                    checkInEntity.key = key;
                    checkInEntity.programId = programId;
                    checkInEntity.memberId = memberId;
                } else {
                    CheckInRecord existing = checkInEntity.toProp().getCheckInRecord(sessionDateYYYYMMDD);
                    if ((null != existing) && (existing.isCheckin)) {
                        String errMessage =
                                "Already checked in by "
                                        + existing.login
                                        + " "
                                        + DateUtils.getDateDiff(existing.timestamp.getTime() / 1000,
                                        new Date().getTime() / 1000) + " ago";
                        throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                                errMessage);
                    }
                }

                CheckInRecord record = new CheckInRecord();
                record.batchNo = batchNo;
                record.isCheckin = true;
                record.login = login;
                record.sessionDateYYYYMMDD = sessionDateYYYYMMDD;
                record.timestamp = new Date();

                checkInEntity.checkInRecords.add(record);
                ofy(client).save().entity(checkInEntity).now();

                return checkInEntity.toProp();
            }
        });

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);
        if (checkInProp.getNumCheckins() == programProp.getNumSessions())
            Member.addOrDeleteProgram(client, memberId, programId, true, login);
        else {
            Member.addOrDeleteProgram(client, memberId, programId, false, login);
        }

        String counterKey = getCheckInCounterKey(programId, sessionDateYYYYMMDD, batchNo);
        long numCheckIns = Counter.incrementAndGetCurrentCount(client, CounterType.CHECKIN, counterKey);
        return (int) numCheckIns;
    }

    @Override
    public int checkout(final long memberId, final long programId, final int sessionDateYYYYMMDD,
                        final String login) {

        ensureValidCheckInInputs(memberId, programId, sessionDateYYYYMMDD, 1, login);

        final String key = getCheckInKey(programId, memberId);

        Integer batchNo = ofy(client).transact(new Work<Integer>() {

            @Override
            public Integer run() {
                CheckInEntity checkInEntity = ofy(client).load().type(CheckInEntity.class).id(key).now();

                if (null == checkInEntity)
                    throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                            "Member [" + memberId + "] is not checked in");

                CheckInRecord existing = checkInEntity.toProp().getCheckInRecord(sessionDateYYYYMMDD);
                if (null == existing)
                    throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                            "Member [" + memberId + "] is not checked in");

                if (!existing.isCheckin) {
                    String errMessage =
                            "Already checked out by "
                                    + existing.login
                                    + " "
                                    + DateUtils.getDateDiff(existing.timestamp.getTime() / 1000,
                                    new Date().getTime() / 1000) + " ago";
                    throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(errMessage);
                }

                CheckInRecord record = new CheckInRecord();
                record.batchNo = existing.batchNo;
                record.isCheckin = false;
                record.login = login;
                record.sessionDateYYYYMMDD = sessionDateYYYYMMDD;
                record.timestamp = new Date();

                checkInEntity.checkInRecords.add(record);
                ofy(client).save().entity(checkInEntity);

                return existing.batchNo;
            }
        });

        // remove the program from member
        Member.addOrDeleteProgram(client, memberId, programId, false, login);

        String counterKey = getCheckInCounterKey(programId, sessionDateYYYYMMDD, batchNo);
        long numCheckIns =
                Counter.incrementAndGetCurrentCount(client, CounterType.CHECKIN, counterKey, -1);
        return (int) numCheckIns;
    }

    @Override
    public int getNumCheckins(long programId, int sessionDateYYYYMMDD, int batchNo) {

        ensureValidCheckInInputs(programId, sessionDateYYYYMMDD, batchNo);
        String counterKey = getCheckInCounterKey(programId, sessionDateYYYYMMDD, batchNo);

        long numCheckIns = Counter.getCount(client, CounterType.CHECKIN, counterKey);
        return (int) numCheckIns;
    }

    public List<CheckInStatusProp> getCheckInStatus(long programId, List<Long> memberIds,
                                                    int sessionDateYYYYMMDD) {

        List<CheckInStatusProp> result = new ArrayList<>();
        // result will have the same size as memberIds
        List<String> checkInKeys = new ArrayList<>();

        for (int i = 0; i < memberIds.size(); i++) {
            CheckInStatusProp checkInStatusProp = new CheckInStatusProp();
            checkInStatusProp.checkedIn = false;
            checkInStatusProp.userFriendlyMessage = "Not checked in";
            result.add(checkInStatusProp);
            checkInKeys.add(getCheckInKey(programId, memberIds.get(i)));
        }

        Map<String, CheckInEntity> map = ofy(client).load().type(CheckInEntity.class).ids(checkInKeys);

        for (int i = 0; i < memberIds.size(); i++) {
            String checkinKey = checkInKeys.get(i);

            if (map.containsKey(checkinKey)) {
                CheckInEntity checkInEntity = map.get(checkinKey);

                if (checkInEntity != null) {
                    CheckInRecord existing = checkInEntity.toProp().getCheckInRecord(sessionDateYYYYMMDD);
                    if ((existing != null) && existing.isCheckin) {
                        CheckInStatusProp checkInStatusProp = result.get(i);
                        checkInStatusProp.checkedIn = true;
                        checkInStatusProp.userFriendlyMessage =
                                "Checked in by "
                                        + existing.login
                                        + " "
                                        + DateUtils.getDateDiff(existing.timestamp.getTime() / 1000,
                                        new Date().getTime() / 1000) + " ago";
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<CheckInMemberProp> getMembersForCheckIn(String searchStr, long programId,
                                                        int sessionDateYYYYMMDD, int maxResultSize, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        if ((searchStr == null) || (searchStr.length() < 3))
            Utils.throwIncorrectSpecException("Atleast 3 chars required for search");

        Program.safeGet(client, programId).toProp(client);

        MemberQueryCondition qc = new MemberQueryCondition(client, maxResultSize);
        qc.searchStr = searchStr.toLowerCase();

        List<MemberProp> memberProps = MemberLoader.querySortedProps(qc, login);

        System.out.println("memberProps.size(): " + memberProps.size());

        List<CheckInMemberProp> checkInMemberProps = new ArrayList<>();
        List<Long> memberIds = new ArrayList<>();
        for (MemberProp memberProp : memberProps) {
            memberIds.add(memberProp.memberId);
        }

        List<CheckInStatusProp> checkInStatus =
                getCheckInStatus(programId, memberIds, sessionDateYYYYMMDD);

        for (int i = 0; i < memberIds.size(); i++) {

            CheckInMemberProp checkInMemberProp = new CheckInMemberProp();

            MemberProp memberProp = memberProps.get(i);
            checkInMemberProp.memberId = memberProp.memberId;
            checkInMemberProp.name = memberProp.contact.getName();
            checkInMemberProp.email = memberProp.contact.email;
            checkInMemberProp.phoneNos = memberProp.contact.getPhoneNos();
            checkInMemberProp.practiceIds = memberProp.practiceIds;
            checkInMemberProp.allow = true;

            CheckInStatusProp checkInStatusProp = checkInStatus.get(i);
            if (checkInStatusProp.checkedIn) {
                checkInMemberProp.allow = false;
                checkInMemberProp.notAllowingReason = checkInStatusProp.userFriendlyMessage;
            }

            checkInMemberProps.add(checkInMemberProp);
        }

        CheckInMemberProp.populatePractices(client, checkInMemberProps);

        // TODO: handle the case where the program requires registration
        // add fields bool requireRegistration and Set<Long>
        // registeredProgramIds in member entity
        return checkInMemberProps;
    }

    protected void ensureValidCheckInInputs(long memberId, long programId, int sessionDateYYYYMMDD,
                                            int batchNo, String login) {

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);

        User.ensureGroupLevelPrivilege(client, programProp.groupProp.groupId, login,
                GroupLevelPrivilege.CHECK_IN);

        MemberLoader.safeGet(client, memberId, login);

        DateUtils.ensureFormatYYYYMMDD(sessionDateYYYYMMDD);

        if ((sessionDateYYYYMMDD < programProp.startYYYYMMDD)
                || (sessionDateYYYYMMDD > programProp.endYYYYMMDD))
            Utils.throwIncorrectSpecException("session date [" + sessionDateYYYYMMDD
                    + "] should be between program start [" + programProp.startYYYYMMDD + "] and end date ["
                    + programProp.endYYYYMMDD + "]");

        if (batchNo > programProp.numBatches)
            Utils.throwIncorrectSpecException("Batch number [" + batchNo
                    + "] is greater than total number of batches [" + programProp.numBatches + "]");
    }

    protected void ensureValidCheckInInputs(long programId, int sessionDateYYYYMMDD, int batchNo) {

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);

        DateUtils.ensureFormatYYYYMMDD(sessionDateYYYYMMDD);

        if ((sessionDateYYYYMMDD < programProp.startYYYYMMDD)
                || (sessionDateYYYYMMDD > programProp.endYYYYMMDD))
            Utils.throwIncorrectSpecException("session date [" + sessionDateYYYYMMDD
                    + "] should be between program start [" + programProp.startYYYYMMDD + "] and end date ["
                    + programProp.endYYYYMMDD + "]");

        if (batchNo > programProp.numBatches)
            Utils.throwIncorrectSpecException("Batch number [" + batchNo
                    + "] is greater than total number of batches [" + programProp.numBatches + "]");
    }

    protected String getCheckInKey(long programId, long memberId) {
        return programId + "_" + memberId;
    }

    protected String getCheckInCounterKey(long programId, long sessionDateYYYYMMDD, int batchNo) {
        return programId + "_" + sessionDateYYYYMMDD + "_" + batchNo;
    }
}
