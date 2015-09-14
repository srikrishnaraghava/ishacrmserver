package crmdna.program;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.programtype.ProgramType;
import crmdna.registration.Registration;
import crmdna.registration.RegistrationSummaryProp;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.teacher.Teacher;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserCore;
import crmdna.user.UserHelper;
import crmdna.venue.Venue;

import java.util.*;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.OfyService.ofy;

public class Program {

    public static ProgramProp create(String client, long groupId, long programTypeId, long venueId,
                                     long teacherId, int startYYYYMMDD, int endYYYYMMDD, int numBatches, String description,
                                     double fees, Currency ccy, String login) {

        Client.ensureValid(client);
        User.ensureGroupLevelPrivilege(client, groupId, login, GroupLevelPrivilege.UPDATE_PROGRAM);

        Group.safeGet(client, groupId);
        ProgramType.safeGet(client, programTypeId);
        Venue.safeGet(client, venueId);
        Teacher.safeGet(client, teacherId);

        ProgramEntity programEntity = new ProgramEntity();
        programEntity.programTypeId = programTypeId;
        programEntity.venueId = venueId;
        programEntity.groupId = groupId;
        programEntity.teacherId = teacherId;
        programEntity.startYYYYMMDD = startYYYYMMDD;
        programEntity.endYYYYMMDD = endYYYYMMDD;
        programEntity.numBatches = numBatches;
        programEntity.description = description;
        programEntity.fee = fees;
        programEntity.ccy = ccy;

        ensureValid(programEntity);

        ensureNotPresentInDB(client, programEntity);
        safeAddToMemcache(client, programEntity);

        programEntity.programId = Sequence.getNext(client, SequenceType.PROGRAM);

        ofy(client).save().entity(programEntity).now();
        return programEntity.toProp(client);
    }

    public static ProgramProp setSpecialInstruction(final String client, final long programId,
                                                    final String specialInstruction) {
        Client.ensureValid(client);

        ProgramEntity programEntity = safeGet(client, programId);

        // can specify null to preserve the existing special instruction
        if (null == specialInstruction)
            return programEntity.toProp(client);

        programEntity.specialInstruction = specialInstruction;

        // Note: there is a minute change of race condition when saving
        ofy(client).save().entity(programEntity).now();
        return programEntity.toProp(client);
    }

    public static ProgramProp setSessionTimings(final String client, long programId,
                                                List<String> batch1SessionTimings, List<String> batch2SessionTimings,
                                                List<String> batch3SessionTimings, List<String> batch4SessionTimings,
                                                List<String> batch5SessionTimings) {

        Client.ensureValid(client);

        // Note: There is a tiny chance of race condition when multiple requests
        // update
        // program entity in the same time though it is likely to be extremely
        // rare as program entities
        // are not updated frequently. Currently we are unable to wrap this in
        // an objectify transaction
        // as loading program entity touches multiple entities and we hit the
        // cross group transaction entity limit

        ProgramEntity programEntity = safeGet(client, programId);

        if (programEntity.numBatches == 1) {
            batch2SessionTimings = null;
            batch3SessionTimings = null;
            batch4SessionTimings = null;
            batch5SessionTimings = null;
        } else if (programEntity.numBatches == 2) {
            batch3SessionTimings = null;
            batch4SessionTimings = null;
            batch5SessionTimings = null;
        } else if (programEntity.numBatches == 3) {
            batch4SessionTimings = null;
            batch5SessionTimings = null;
        } else if (programEntity.numBatches == 4) {
            batch5SessionTimings = null;
        }

        boolean changed = false;
        // can specify null to preserve existing value
        if ((batch1SessionTimings != null) && !batch1SessionTimings.isEmpty()) {
            programEntity.batch1SessionTimings = batch1SessionTimings;
            changed = true;
        }

        if ((batch2SessionTimings != null) && !batch2SessionTimings.isEmpty()) {
            programEntity.batch2SessionTimings = batch2SessionTimings;
            changed = true;
        }

        if ((batch3SessionTimings != null) && !batch3SessionTimings.isEmpty()) {
            programEntity.batch3SessionTimings = batch3SessionTimings;
            changed = true;
        }

        if ((batch4SessionTimings != null) && !batch4SessionTimings.isEmpty()) {
            programEntity.batch4SessionTimings = batch4SessionTimings;
            changed = true;
        }

        if ((batch5SessionTimings != null) && batch5SessionTimings.isEmpty()) {
            programEntity.batch5SessionTimings = batch5SessionTimings;
            changed = true;
        }

        if (!changed)
            return programEntity.toProp(client);

        ofy(client).save().entity(programEntity).now();
        return programEntity.toProp(client);
    }

    public static ProgramProp setMaxParticipants(final String client, long programId,
                                                 Integer maxParticipants) {

        Client.ensureValid(client);

        ProgramEntity programEntity = safeGet(client, programId);

        if (maxParticipants != null) {
            programEntity.maxParticipants = maxParticipants;
            ofy(client).save().entity(programEntity).now();
        }

        return programEntity.toProp(client);
    }

    public static ProgramProp setDisabled(final String client, long programId, Boolean disabled) {

        Client.ensureValid(client);

        ProgramEntity programEntity = safeGet(client, programId);

        if (disabled != null) {
            programEntity.disabled = disabled;
            ofy(client).save().entity(programEntity).now();
        }

        return programEntity.toProp(client);
    }


    public static Map<Long, ProgramProp> getProps(String client, Iterable<Long> programIds) {

        Client.ensureValid(client);

        Map<Long, ProgramEntity> programEntities = getEntities(client, programIds);

        Map<Long, ProgramProp> programProps = new HashMap<>();

        for (Long programId : programEntities.keySet()) {
            ProgramEntity programEntity = programEntities.get(programId);

            if (programEntity != null)
                programProps.put(programId, programEntity.toProp(client));
        }

        return programProps;
    }

    public static Map<Long, ProgramEntity> getEntities(String client, Iterable<Long> programIds) {

        Client.ensureValid(client);

        Map<Long, ProgramEntity> map = ofy(client).load().type(ProgramEntity.class).ids(programIds);

        return map;
    }

    public static ProgramProp update(String client, long programId, Long newVenueId,
                                     Long newTeacherId, Integer newStartYYYYMMDD, Integer newEndYYYYMMDD, Integer newNumBatches,
                                     String newDescription, Double fees, Currency ccy, String login) {

        // warning: this method will modify session cache if an exception is
        // thrown

        Client.ensureValid(client);
        ProgramEntity programEntity = safeGet(client, programId);
        ProgramProp programProp = programEntity.toProp(client);

        User.ensureGroupLevelPrivilege(client, programProp.groupProp.groupId, login,
                GroupLevelPrivilege.UPDATE_PROGRAM);

        boolean checkUnique = false;
        if (null != newVenueId) {
            if (newVenueId != programProp.venueProp.venueId) {
                Venue.safeGet(client, newVenueId);
                programEntity.venueId = newVenueId;
                checkUnique = true;
            }
        }

        if (null != newTeacherId) {
            if (newTeacherId != programProp.teacherProp.teacherId) {
                Teacher.safeGet(client, newTeacherId);
                programEntity.teacherId = newTeacherId;
            }
        }

        if (null != newStartYYYYMMDD) {
            if (newStartYYYYMMDD != programProp.startYYYYMMDD) {
                DateUtils.ensureFormatYYYYMMDD(newStartYYYYMMDD);
                programEntity.startYYYYMMDD = newStartYYYYMMDD;
                checkUnique = true;
            }
        }

        if (null != newEndYYYYMMDD) {
            if (newEndYYYYMMDD != programProp.endYYYYMMDD) {
                DateUtils.ensureFormatYYYYMMDD(newEndYYYYMMDD);
                programEntity.endYYYYMMDD = newEndYYYYMMDD;
                checkUnique = true;
            }
        }

        if (null != newNumBatches) {
            programEntity.numBatches = newNumBatches;
        }

        if (null != newDescription)
            programEntity.description = newDescription;

        if (null != fees)
            programEntity.fee = fees;

        if (null != ccy)
            programEntity.ccy = ccy;

        ensureValid(programEntity);

        if (checkUnique) {
            ensureNotPresentInDB(client, programEntity);
            safeAddToMemcache(client, programEntity);
        }

        ofy(client).save().entity(programEntity).now();
        return programEntity.toProp(client);
    }

    private static void ensureValid(ProgramEntity programEntity) {
        DateUtils.ensureFormatYYYYMMDD(programEntity.startYYYYMMDD);
        DateUtils.ensureFormatYYYYMMDD(programEntity.endYYYYMMDD);

        if (programEntity.startYYYYMMDD > programEntity.endYYYYMMDD)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Start date [" + programEntity.startYYYYMMDD + "] is greater than end date ["
                            + programEntity.endYYYYMMDD + "]");

        if (programEntity.numBatches < 1)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid number of batches [" + programEntity.numBatches
                            + "]. numBatches should be positive");

        if ((programEntity.programTypeId == 0) || (programEntity.venueId == 0)
                || (programEntity.teacherId == 0) || (programEntity.groupId == 0))
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Either program type or venue or teacher or group not populated");

        Utils.ensureNonNegative(programEntity.fee);

        if (programEntity.fee != 0)
            if (programEntity.ccy == null)
                Utils.throwIncorrectSpecException("Ccy should be specified");
    }

    public static ProgramEntity safeGet(String client, long programId) {
        Client.ensureValid(client);

        ProgramEntity entity = ofy(client).load().type(ProgramEntity.class).id(programId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "There is no program with id [" + programId + "]");

        return entity;
    }

    public static ProgramEntity get(String client, long programId) {
        Client.ensureValid(client);

        ProgramEntity entity = ofy(client).load().type(ProgramEntity.class).id(programId).now();

        return entity;
    }

    public static List<ProgramProp> query(String client, Integer startYYYYMMDD, Integer endYYYYMMDD,
                                          Set<Long> programTypeIds, Set<Long> groupIds, Long venueId, Integer maxResultSize) {
        Client.ensureValid(client);

        Query<ProgramEntity> q = ofy(client).load().type(ProgramEntity.class);

        if (null != startYYYYMMDD)
            DateUtils.ensureFormatYYYYMMDD(startYYYYMMDD);

        if (null != endYYYYMMDD)
            DateUtils.ensureFormatYYYYMMDD(endYYYYMMDD);

        if ((null != programTypeIds) && (programTypeIds.size() != 0)) {
            q = q.filter("programTypeId in", programTypeIds);
        }

        if ((null != groupIds) && (groupIds.size() != 0)) {
            q = q.filter("groupId in", groupIds);
        }

        if (null != venueId) {
            Venue.safeGet(client, venueId);
            q = q.filter("venueId", venueId);
        }

        // don't limit the query as this will mess up the sort order.
        // get all results, sort and then limit the data that is returned

        List<ProgramEntity> entities = q.list();

        List<ProgramProp> props = new ArrayList<>();
        for (ProgramEntity entity : entities) {
            // discard entities that are after end date (if specified)
            if ((null != endYYYYMMDD) && (entity.endYYYYMMDD > endYYYYMMDD))
                continue;

            if ((null != startYYYYMMDD) && (entity.startYYYYMMDD < startYYYYMMDD))
                continue;

            RegistrationSummaryProp registrationSummaryProp =
                    Registration.getSummary(client, entity.programId, User.SUPER_USER);
            ProgramProp programProp = entity.toProp(client);
            programProp.isRegistrationLimitReached =
                    (registrationSummaryProp.numCompleted >= entity.maxParticipants);

            props.add(programProp);
        }

        Collections.sort(props);

        if ((maxResultSize != null) && (props.size() > maxResultSize))
            props = props.subList(0, maxResultSize);

        return props;
    }

    static List<ProgramProp> getOngoingPrograms(String client, int dateYYYYMMDD, String login) {
        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        DateUtils.ensureFormatYYYYMMDD(dateYYYYMMDD);

        Set<Long> groupIds =
                UserHelper.getGroupIdsWithPrivilage(client, login, GroupLevelPrivilege.CHECK_IN);

        // remove groupId 0 as it is not a valid group
        groupIds.remove((long) 0);

        // if user does not have write access to any group just return
        if (0 == groupIds.size())
            return new ArrayList<>();

        List<ProgramProp> programProps = query(client, null, null, null, groupIds, null, null);

        List<ProgramProp> ongoing = new ArrayList<>();
        for (ProgramProp programProp : programProps) {
            if ((dateYYYYMMDD >= programProp.startYYYYMMDD) && (dateYYYYMMDD <= programProp.endYYYYMMDD))
                ongoing.add(programProp);
        }

        return ongoing;
    }

    public static List<SessionProp> getOngoingSessions(String client, int dateYYYYMMDD, String login) {
        List<ProgramProp> programProps = getOngoingPrograms(client, dateYYYYMMDD, login);

        List<SessionProp> sessionProps = new ArrayList<>();
        for (ProgramProp programProp : programProps) {
            sessionProps.addAll(programProp.getSessions(dateYYYYMMDD));
        }

        return sessionProps;
    }

    private static String getUniqueKey(String namespace, ProgramEntity programEntity) {

        return namespace + "_" + programEntity.programTypeId + "_"
                + programEntity.venueId + "_" + programEntity.startYYYYMMDD + "_"
                + programEntity.endYYYYMMDD;
    }

    private static void ensureNotPresentInDB(String namespace, ProgramEntity programEntity) {
        List<Key<ProgramEntity>> keys =
                ofy(namespace).load().type(ProgramEntity.class)
                        .filter("programTypeId", programEntity.programTypeId)
                        .filter("venueId", programEntity.venueId)
                        .filter("startYYYYMMDD", programEntity.startYYYYMMDD)
                        .filter("endYYYYMMDD", programEntity.endYYYYMMDD).keys().list();

        if (keys.size() != 0)
            throw new APIException()
                    .status(Status.ERROR_RESOURCE_ALREADY_EXISTS)
                    .message(
                            "There is already a program with the same start date, end date, venue, program type and batch.");
    }

    private static void safeAddToMemcache(String namespace, ProgramEntity programEntity) {
        String key = getUniqueKey(namespace, programEntity);

        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException()
                    .status(Status.ERROR_RESOURCE_ALREADY_EXISTS)
                    .message(
                            "There is already a program with the same start date, end date, venue, program type and batch. ");
    }

    // this should be removed after bhairavi data migration is complete
    public static void resaveAll(String client, String login) {
        Client.ensureValid(client);

        ensure(UserCore.isSuperUser(login), "Allowed only for super user");

        List<ProgramEntity> all = ofy(client).load().type(ProgramEntity.class).list();

        ofy(client).save().entities(all).now();
    }
}
