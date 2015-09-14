package crmdna.interaction;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.common.DateUtils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class InteractionCore {

    public final static long MAX_CONTENT_SIZE = 5 * 1024;
    public final static long MAX_SUB_INTERACTIONS = 99;

    static InteractionProp createInteraction(String client, long memberId, String content,
                                             String interactionType, Date timestamp, long userId,
                                             Long campaignId, String user) {

        ensureNotNull(timestamp, "timestamp is null");
        ensureNotNull(interactionType, "interactionType is null");
        ensure(interactionType.length() != 0, "interactionType is empty");
        ensureNotNull(user, "userEmail is null");
        ensure(user.length() != 0, "userEmail is empty");

        if (content.length() > MAX_CONTENT_SIZE) {
            throw new APIException().status(Status.ERROR_OVERFLOW)
                    .message("Interaction content cannot have more than ["
                            + MAX_CONTENT_SIZE + "] characters");
        }

        InteractionEntity interactionEntity = new InteractionEntity();
        interactionEntity.interactionId = Sequence.getNext(client, SequenceType.INTERACTION);
        long us = DateUtils.getMicroSeconds(timestamp);
        interactionEntity.subInteractions.put(us, content);
        interactionEntity.ms = timestamp.getTime();
        interactionEntity.memberId = memberId;
        interactionEntity.userId = userId;
        interactionEntity.user = user;
        interactionEntity.interactionType = interactionType;
        interactionEntity.campaignId = campaignId;

        ofy(client).save().entity(interactionEntity).now();
        return interactionEntity.toProp();
    }

    static InteractionProp updateInteraction(String client, long interactionId, Long newMemberId,
                                             String newInteractionType, Long newUserId, String newUser) {

        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");
        ensure(interactionId != 0, "interactionId is 0");

        InteractionEntity interactionEntity = safeGet(client, interactionId);

        if (newMemberId != null) {
            ensure(newMemberId != 0, "newMemberId is 0");
            interactionEntity.memberId = newMemberId;
        }

        if ((newInteractionType != null) && (newInteractionType.length() != 0))
            interactionEntity.interactionType = newInteractionType;

        if (newUserId != null) {
            ensure(newUserId != 0, "newUserId is 0");
            interactionEntity.userId = newUserId;
        }

        if (newUser != null) {
            ensure(newUser.length() != 0, "newUser is empty");
            interactionEntity.user = newUser;
        }

        ofy(client).save().entity(interactionEntity);

        return interactionEntity.toProp();
    }

    static void deleteInteraction(String client, long interactionId) {
        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");
        ensure(interactionId != 0, "interaction id is 0");

        InteractionEntity interactionEntity = safeGet(client, interactionId);
        ofy(client).delete().entity(interactionEntity);
    }

    static void deleteSubInteraction(String client, long interactionId, long subInteractionId) {
        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");
        ensure(interactionId != 0, "interaction id is 0");
        ensure(subInteractionId != 0, "subInteraction id is 0");

        InteractionEntity interactionEntity = safeGet(client, interactionId);

        if (!interactionEntity.subInteractions.containsKey(subInteractionId))
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "subInteractionId [" + subInteractionId + "] not found in interaction [" + interactionId
                            + "]");

        interactionEntity.subInteractions.remove(subInteractionId);

        ofy(client).save().entity(interactionEntity);
    }

    static void createSubInteraction(String client, long interactionId, String content, Date timestamp) {

        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");

        ensureNotNull(content, "content is null");
        if (content.length() > MAX_CONTENT_SIZE) {
            throw new APIException().status(Status.ERROR_OVERFLOW)
                    .message("Sub interaction cannot have more than ["
                            + MAX_CONTENT_SIZE + "] characters");
        }

        InteractionEntity interactionEntity = safeGet(client, interactionId);

        if (interactionEntity.subInteractions.size() >= MAX_SUB_INTERACTIONS)
            throw new APIException().status(Status.ERROR_OVERFLOW).message(
                    "Cannot add more than [" + MAX_SUB_INTERACTIONS
                            + "] sub interactions for a single interaction");

        long us = DateUtils.getMicroSeconds(timestamp);

        if (interactionEntity.subInteractions.containsKey(us))
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a sub interaction for timestamp [" + timestamp + "]");

        interactionEntity.subInteractions.put(us, content);

        // update time stamp in interactionEntity if later
        interactionEntity.ms = Math.max(interactionEntity.ms, timestamp.getTime());

        ofy(client).save().entity(interactionEntity);
    }

    static void updateSubInteraction(String client, long interactionId, long subInteractionId,
                                     String content, Date timestamp) {

        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");

        ensureNotNull(content, "content is null");
        if (content.length() > MAX_CONTENT_SIZE) {
            throw new APIException().status(Status.ERROR_OVERFLOW)
                    .message("Sub interaction cannot have more than ["
                            + MAX_CONTENT_SIZE + "] characters");
        }

        InteractionEntity interactionEntity = safeGet(client, interactionId);

        if (!interactionEntity.subInteractions.containsKey(subInteractionId))
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "There is no sub interaction with id [" + subInteractionId + "]");

        if (timestamp == null) {
            // update contents for the same sub interaction
            interactionEntity.subInteractions.put(subInteractionId, content);
        } else {
            interactionEntity.subInteractions.remove(subInteractionId);
            long us = DateUtils.getMicroSeconds(timestamp);
            interactionEntity.subInteractions.put(us, content);

            interactionEntity.ms = Math.max(interactionEntity.ms, timestamp.getTime());
        }

        ofy(client).save().entity(interactionEntity);
    }

    static InteractionQueryResult query(String client, InteractionQueryCondition qc) {

        List<Key<InteractionEntity>> keys = queryKeys(client, qc);

        InteractionQueryResult qr = new InteractionQueryResult();
        qr.interactionQueryCondtion = qc;
        qr.totalSize = keys.size();

        if (qc.startIndex == null) {
            ensure(qc.numResults == null,
                    "numResults should not be specified when startIndex is not specified");
            Map<Key<InteractionEntity>, InteractionEntity> map = ofy(client).load().keys(keys);

            List<InteractionProp> props = new ArrayList<>(keys.size());
            for (Key<InteractionEntity> key : keys) {
                props.add(map.get(key).toProp());
            }

            qr.interactionProps = props;
            return qr;
        }

        // some keys need to be excluded
        ensure(qc.startIndex >= 0, "Invalid startIndex [" + qc.startIndex + "]");
        ensureNotNull(qc.numResults, "numResults should be specified when startIndex is specified");
        ensure(qc.numResults > 0, "Invalid numResults [" + qc.numResults + "]");

        if (qc.startIndex + 1 > keys.size()) {
            // no results to be fetched
            return qr;
        }

        ensure(keys.size() >= qc.startIndex);

        int numResults = Math.min(qc.numResults, keys.size() - qc.startIndex);
        ensure(numResults <= keys.size());
        ensure(numResults > 0);

        int endIndex = qc.startIndex + numResults;
        ensure(endIndex <= keys.size());
        ensure(endIndex > qc.startIndex);

        keys = keys.subList(qc.startIndex, endIndex);

        Map<Key<InteractionEntity>, InteractionEntity> map = ofy(client).load().keys(keys);

        List<InteractionProp> props = new ArrayList<>(keys.size());
        for (Key<InteractionEntity> key : keys) {
            props.add(map.get(key).toProp());
        }

        qr.interactionProps = props;
        return qr;
    }

    static List<Key<InteractionEntity>> queryKeys(String client, InteractionQueryCondition qc) {

        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");

        ensureNotNull(qc, "InteractionQueryCondition is null");

        Query<InteractionEntity> q = ofy(client).load().type(InteractionEntity.class);

        if (qc.interactionTypes != null)
            if (!qc.interactionTypes.isEmpty())
                q = q.filter("interactionType in", qc.interactionTypes);

        if (qc.memberIds != null)
            if (!qc.memberIds.isEmpty())
                q = q.filter("memberId in", qc.memberIds);

        if (qc.userIds != null)
            if (!qc.userIds.isEmpty())
                q = q.filter("userId in", qc.userIds);

        if (qc.start != null) {
            q = q.filter("ms >=", qc.start.getTime());
        }

        if (qc.end != null) {
            q = q.filter("ms <=", qc.end.getTime());
        }

        if (qc.campaignIds != null && !qc.campaignIds.isEmpty()) {
            q = q.filter("campaignId in", qc.campaignIds);
        }

        List<Key<InteractionEntity>> keys = q.order("-ms").keys().list();

        return keys;
    }

    public static InteractionEntity safeGet(String client, long interactionId) {

        ensure(interactionId != 0, "interactionId is 0");
        InteractionEntity interactionEntity =
                ofy(client).load().type(InteractionEntity.class).id(interactionId).now();

        if (null == interactionEntity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Interaction id [" + interactionId + "] does not exist");

        return interactionEntity;
    }
}
