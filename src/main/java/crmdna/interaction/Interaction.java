package crmdna.interaction;

import crmdna.calling.Campaign;
import crmdna.calling.CampaignProp;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.member.MemberLoader;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.Date;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;

public class Interaction {

    public static InteractionProp createInteraction(String client, long memberId, String content,
                                                    InteractionType interactionType, Date timestamp,
                                                    Long campaignId, boolean ensureWithinCampaignDates, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        ensureNotNull(interactionType, "interactionType is null");

        MemberLoader.safeGet(client, memberId, login);
        long userId = User.safeGet(client, login).toProp(client).userId;

        if (timestamp == null)
            timestamp = new Date(); // current time

        if (campaignId != null) {
            CampaignProp campaignProp = Campaign.safeGet(client, campaignId).toProp();
            if (ensureWithinCampaignDates) {
                long yyyymmdd = DateUtils.toYYYYMMDD(timestamp);
                if (yyyymmdd < campaignProp.startYYYYMMDD || yyyymmdd > campaignProp.endYYYYMMDD) {
                    throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_INCORRECT)
                            .object("Timestamp does not fall with campaign start [" + campaignProp.startYYYYMMDD +
                                "] and end [" + campaignProp.endYYYYMMDD +
                                "] dates. To suppress this validation change flag ensureWithinCampaignDates to false");
                }
            }

            ensure(campaignProp.enabled, "Campaign [" + campaignProp.campaignId + "] is disabled");
        }

        InteractionProp interactionProp =
                InteractionCore.createInteraction(client, memberId, content, interactionType.toString(),
                        timestamp, userId, campaignId, login);

        //interaction score is anyway approximate. no need to update score in a transaction
        InteractionScore.incrementBy1(client, login, memberId);

        return interactionProp;
    }

    public static void deleteInteraction(String client, long interactionId, String login) {
        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        long userId = User.safeGet(client, login).toProp(client).userId;

        ensure(interactionId != 0, "interaction id is 0");

        InteractionEntity entity = InteractionCore.safeGet(client, interactionId);

        // need previlage for deleting someone else's interaction
        if (entity.userId != userId) {
            User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INTERACTION);
        }

        InteractionCore.deleteInteraction(client, interactionId);
    }

    public static InteractionProp updateInteraction(String client, long interactionId,
                                                    Long newMemberId, InteractionType newInteractionType, String newUser, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        InteractionEntity entity = InteractionCore.safeGet(client, interactionId);

        if (newMemberId != null)
            MemberLoader.safeGet(client, newMemberId, login);

        long userId = User.safeGet(client, login).toProp(client).userId;

        // need privilege to update someone else's interaction
        if (entity.userId != userId)
            User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INTERACTION);

        Long newUserId = null;
        if (newUser != null) {
            newUserId = User.safeGet(client, newUser).toProp(client).userId;
            ensureNotNull(newUserId);

            if (newUserId != entity.userId)
                User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INTERACTION);
        }

        return InteractionCore.updateInteraction(client, interactionId, newMemberId,
                newInteractionType != null ? newInteractionType.toString() : null, newUserId, newUser);
    }

    public static InteractionQueryResult query(String client, InteractionQueryCondition qc,
                                               String login) {
        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        return InteractionCore.query(client, qc);
    }

    public static int count(String client, InteractionQueryCondition qc) {
        return InteractionCore.queryKeys(client, qc).size();
    }

    public static void createSubInteraction(String client, long interactionId, String content,
                                            Date timestamp, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        ensure(interactionId != 0, "interactionId is 0");

        InteractionEntity interactionEntity = InteractionCore.safeGet(client, interactionId);

        long userId = User.safeGet(client, login).toProp(client).userId;

        if (interactionEntity.userId != userId) {
            User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INTERACTION);
        }

        if (null == timestamp)
            timestamp = new Date();

        InteractionCore.createSubInteraction(client, interactionId, content, timestamp);

        //interaction score is anyway approximate. no need to update score in a transaction
        InteractionScore.incrementBy1(client, login, interactionEntity.memberId);
    }

    public static void deleteSubInteraction(String client, long interactionId, long subinteractionId,
                                            String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        ensure(interactionId != 0, "interactionId is 0");

        InteractionEntity entity = InteractionCore.safeGet(client, interactionId);

        long userId = User.safeGet(client, login).toProp(client).userId;

        if (entity.userId != userId)
            User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INTERACTION);

        InteractionCore.deleteSubInteraction(client, interactionId, subinteractionId);
    }

    public static void updateSubInteraction(String client, long interactionId, long subInteractionId,
                                            String content, String login) {

        Client.ensureValid(client);

        InteractionEntity interactionEntity = InteractionCore.safeGet(client, interactionId);

        long userId = User.safeGet(client, login).toProp(client).userId;

        if (interactionEntity.userId != userId)
            User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INTERACTION);

        InteractionCore.updateSubInteraction(client, interactionId, subInteractionId, content, null);
    }

    public enum InteractionType {
        PHONE, EMAIL, FB, GOOGLEPLUS, SMS, FACETOFACE, WEBSITE, OTHER
    }
}
