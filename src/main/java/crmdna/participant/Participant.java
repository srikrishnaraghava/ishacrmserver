package crmdna.participant;

import com.googlecode.objectify.Key;
import crmdna.client.Client;
import crmdna.common.ValidationResultProp;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.Contact;
import crmdna.common.contact.ContactProp;
import crmdna.member.Member;
import crmdna.member.MemberEntity;
import crmdna.member.MemberEntity.MemberFactory;
import crmdna.member.MemberLoader;
import crmdna.participant.ParticipantEntity.ParticipantFactory;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static crmdna.common.AssertUtils.ensureEqual;
import static crmdna.common.OfyService.ofy;
import static crmdna.member.MemberBulkSaver.memberBulkSaver;

public class Participant {

    public static List<ParticipantProp> getAll(String client, long programId, String login) {

        User.ensureValidUser(client, login);

        Program.safeGet(client, programId);

        List<Key<ParticipantEntity>> keys =
                ofy(client).load().type(ParticipantEntity.class).filter("programId", programId).keys()
                        .list();

        Map<Key<ParticipantEntity>, ParticipantEntity> map = ofy(client).load().keys(keys);

        List<ParticipantProp> participantProps = new ArrayList<>();

        for (Key<ParticipantEntity> key : map.keySet()) {
            participantProps.add(map.get(key).toProp());
        }

        Collections.sort(participantProps);

        return participantProps;
    }

    public static int deleteAll(String client, long programId, String login) {

        Client.ensureValid(client);

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);
        User.ensureGroupLevelPrivilege(client, programProp.groupProp.groupId, login,
                GroupLevelPrivilege.UPDATE_PARTICIPANT);

        List<Key<ParticipantEntity>> participantKeys =
                ofy(client).load().type(ParticipantEntity.class).filter("programId", programId).keys()
                        .list();

        if (0 == participantKeys.size()) {
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "No participants data found for programId [" + programId + "]");
        }

        Map<Key<ParticipantEntity>, ParticipantEntity> map = ofy(client).load().keys(participantKeys);

        // delete the program for all member records
        List<Long> memberIds = new ArrayList<>();
        for (Key<ParticipantEntity> key : map.keySet()) {
            ParticipantEntity participantEntity = map.get(key);

            if (participantEntity != null)
                memberIds.add(participantEntity.memberId);
        }

        Map<Long, MemberEntity> memberEntities = MemberLoader.get(client, memberIds, login);

        memberBulkSaver(client, new ArrayList<>(memberEntities.values())).deleteProgramFromAll(programId)
                .populateDependantsAndSave();

        ofy(client).delete().keys(participantKeys);

        Logger logger = Logger.getLogger(Participant.class.getName());
        logger.info("Deleted [" + participantKeys.size() + "] participants. Removed program from ["
                + memberEntities.size() + "] members");

        return participantKeys.size();
    }

    public static UploadReportProp uploadAll(String client, List<ContactProp> contacts,
                                             long programId, boolean ignoreWarnings, boolean updateProfile, String login) {

        // This function will save the entered details as is into participant
        // entities.
        // If the member is new it will create member entities.

        // if participant details are already saved, just
        // throw an error message and exit

        Client.ensureValid(client);
        int count =
                ofy(client).load().type(ParticipantEntity.class).filter("programId", programId).keys()
                        .list().size();

        if (0 != count) {
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "[" + count + "] participants already found for program id [" + programId
                            + "]. These participants should be deleted first before " + " another upload. ");
        }

        // logic is as follows -

        // for existing members
        // 1. add program to memberentity
        // 2. populate member id to participantentity
        // for new members
        // 1. save as members first and get the member id
        // 2. populate member id to participant entity
        // save all participant entities

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);
        User.ensureGroupLevelPrivilege(client, programProp.groupProp.groupId, login,
                GroupLevelPrivilege.UPDATE_PARTICIPANT);

        ValidationResultProp validationResultProp = Contact.validate(contacts);

        if (validationResultProp.hasErrors()) {
            throw new APIException()
                    .status(Status.ERROR_RESOURCE_INCORRECT)
                    .object(validationResultProp)
                    .message(
                            "Found [" + validationResultProp.getNumErrors() + "] errors and ["
                                    + validationResultProp.getNumWarnings()
                                    + "] warnings. Please correct them before uploading");
        }

        if (validationResultProp.hasWarnings() && !ignoreWarnings) {
            throw new APIException()
                    .status(Status.ERROR_RESOURCE_INCORRECT)
                    .object(validationResultProp)
                    .message(
                            "Found [" + validationResultProp.getNumWarnings()
                                    + "] warnings. To ignore warnings set [ignoreWarnings] flag to true");
        }

        List<MemberEntity> memberEntities =
                Member.getMatchingMembersSameSizeList(client, contacts, login);

        List<ContactProp> toAddContactProps = new ArrayList<>();
        List<ContactProp> toUpdateContactProps = new ArrayList<>();
        List<MemberEntity> toUpdateMemberEntities = new ArrayList<>();

        List<Boolean> match = new ArrayList<>(contacts.size());

        for (int index = 0; index < contacts.size(); index++) {
            ContactProp c = contacts.get(index);
            c.asOfyyyymmdd = programProp.endYYYYMMDD;
            MemberEntity memberEntity = memberEntities.get(index);

            if (memberEntity == null) {
                toAddContactProps.add(c);
                match.add(false);
            } else {
                toUpdateContactProps.add(c);
                toUpdateMemberEntities.add(memberEntity);
                match.add(true);

                // never change the member profile email address
                c.email = null;
            }
        }

        ensureEqual(contacts.size(), match.size());
        ensureEqual(toAddContactProps.size() + toUpdateContactProps.size(), contacts.size());

        List<MemberEntity> toAddMemberEntities = MemberFactory.create(client, toAddContactProps.size());

        // save new members
        memberBulkSaver(client, toAddMemberEntities).setContactsSameSizeList(toAddContactProps)
                .addGroupToAll(programProp.groupProp.groupId).addProgramToAll(programId)
                .populateDependantsAndSave();

        // save existing members
        if (updateProfile) {
            // update contact details in member profile. Never change email.
            // email already set to null in one of the above for loops
            memberBulkSaver(client, toUpdateMemberEntities).setContactsSameSizeList(toUpdateContactProps)
                    .addProgramToAll(programId).populateDependantsAndSave();
        } else {
            memberBulkSaver(client, toUpdateMemberEntities).addProgramToAll(programId)
                    .populateDependantsAndSave();
        }

        // populate member id
        memberEntities = new ArrayList<>(contacts.size());
        int addIndex = 0;
        int updateIndex = 0;
        for (int i = 0; i < contacts.size(); i++) {
            MemberEntity memberEntity;
            if (match.get(i)) {
                // already existing member
                memberEntity = toUpdateMemberEntities.get(updateIndex);
                updateIndex++;
            } else {
                // new member
                memberEntity = toAddMemberEntities.get(addIndex);
                addIndex++;
            }

            memberEntities.add(memberEntity);
        }

        List<ParticipantEntity> participantEntities =
                ParticipantFactory.create(client, contacts, memberEntities, programId);
        ensureEqual(participantEntities.size(), contacts.size());

        // save
        ofy(client).save().entities(participantEntities);

        UploadReportProp uploadReportProp = new UploadReportProp();
        uploadReportProp.numParticipants = contacts.size();
        uploadReportProp.numExistingMembers = toUpdateMemberEntities.size();
        uploadReportProp.numNewMembers =
                uploadReportProp.numParticipants - toUpdateMemberEntities.size();

        return uploadReportProp;
    }
}
