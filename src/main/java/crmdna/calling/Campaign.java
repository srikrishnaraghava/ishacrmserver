package crmdna.calling;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.interaction.Interaction;
import crmdna.interaction.InteractionQueryCondition;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.sequence.Sequence;
import crmdna.user.User;

import java.util.List;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.AssertUtils.ensureNotNullNotEmpty;
import static crmdna.common.OfyService.ofy;

/**
 * Created by sathya on 17/8/15.
 */
public class Campaign {

    public static CampaignProp create(String client, long programId, String displayName, int startYYYYMMDD,
                                      int endYYYYMMDD, String login) {
        Client.ensureValid(client);

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);

        ensure(displayName != null);

        ensure(DateUtils.isFormatInYYYYMMDD(startYYYYMMDD),
                "Invalid start date [" + startYYYYMMDD + "]. Should be in format yyyymmdd");
        ensure(DateUtils.isFormatInYYYYMMDD(endYYYYMMDD),
                "Invalid end date [" + endYYYYMMDD + "]. Should be in format yyyymmdd");

        ensure(endYYYYMMDD >= startYYYYMMDD,
                "End date [" + endYYYYMMDD + "] should not be before start date [" + startYYYYMMDD + "]");

        User.ensureGroupLevelPrivilege(client, programProp.groupProp.groupId,
                login, User.GroupLevelPrivilege.UPDATE_CAMPAIGN);

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());
        ensure(name.length() != 0, "Invalid name [" + name + "] after trimming space, bracket, hyphen");

        long numExisting = ofy(client).load().type(CampaignEntity.class).filter("campaignName", name)
                .filter("programId", programProp.programId).list().size();
        if (numExisting != 0) {
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_ALREADY_EXISTS)
                    .object("There is already a campaign [" + name + "] for program [" + programProp.programId + "]");
        }

        CampaignEntity entity = new CampaignEntity();
        entity.campaignId = Sequence.getNext(client, Sequence.SequenceType.CAMPAIGN);
        entity.campaignName = name;
        entity.displayName = displayName;
        entity.enabled = true;
        entity.groupId = programProp.groupProp.groupId;
        entity.programId = programProp.programId;
        entity.startYYYYMMDD = startYYYYMMDD;
        entity.endYYYYMMDD = endYYYYMMDD;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static CampaignEntity safeGet(String client, long campaignId) {
        Client.ensureValid(client);

        CampaignEntity campaignEntity = ofy(client).load().type(CampaignEntity.class).id(campaignId).now();

        if (null == campaignEntity) {
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND)
                    .message("Cannot find campaign for id [" + campaignId + "] for client [" + client + "]");
        }

        return campaignEntity;
    }

    public static CampaignProp enable(String client, long campaignId, boolean enable, String login) {

        Client.ensureValid(client);

        CampaignEntity entity = safeGet(client, campaignId);

        User.ensureGroupLevelPrivilege(client, entity.groupId, login,
                User.GroupLevelPrivilege.UPDATE_CAMPAIGN);

        if (entity.enabled != enable) {
            entity.enabled = enable;
            ofy(client).save().entity(entity).now();
        }

        return entity.toProp();
    }

    public static CampaignProp rename(String client, long campaignId, String newDisplayName, String login) {

        Client.ensureValid(client);
        CampaignEntity entity = Campaign.safeGet(client, campaignId);
        User.ensureGroupLevelPrivilege(client, entity.groupId, login, User.GroupLevelPrivilege.UPDATE_CAMPAIGN);

        ensureNotNullNotEmpty(newDisplayName, "display name not specified");

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());

        if (! name.equals(entity.campaignName)) {
            List<CampaignEntity> campaignProps = ofy(client).load().type(CampaignEntity.class)
                    .filter("campaignName", name)
                    .filter("programId", entity.programId).list();

            if (! campaignProps.isEmpty()) {
                throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_ALREADY_EXISTS)
                        .message("There is already a campaign [" + newDisplayName + "] for program [" + entity.programId + "]");
            }
        }

        entity.displayName = newDisplayName;
        entity.campaignName = name;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static CampaignProp updateDates(String client, long campaignId, Integer newStartYYYYMMDD,
                                           Integer newEndYYYYMMDD, String login) {

        Client.ensureValid(client);
        CampaignEntity entity = Campaign.safeGet(client, campaignId);
        User.ensureGroupLevelPrivilege(client, entity.groupId, login, User.GroupLevelPrivilege.UPDATE_CAMPAIGN);

        if (newStartYYYYMMDD != null) {
            ensure(DateUtils.isFormatInYYYYMMDD(newStartYYYYMMDD),
                    "Specified start date [" + newStartYYYYMMDD + "] is not in yyyymmdd format");
        }

        if (newEndYYYYMMDD != null) {
            ensure(DateUtils.isFormatInYYYYMMDD(newEndYYYYMMDD),
                    "Specified end date [" + newEndYYYYMMDD + "] is not in yyyymmdd format");
        }

        int newStart = (newStartYYYYMMDD != null) ? newStartYYYYMMDD : entity.startYYYYMMDD;
        int newEnd = (newEndYYYYMMDD != null) ? newEndYYYYMMDD : entity.endYYYYMMDD;

        ensure(newEnd >= newStart, "End date [" + newEnd + "] is before start date [" + newStart + "]");

        entity.startYYYYMMDD = newStart;
        entity.endYYYYMMDD = newEnd;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static void delete(String client, long campaignId, String login) {
        Client.ensureValid(client);

        CampaignEntity campaignEntity = safeGet(client, campaignId);
        User.ensureGroupLevelPrivilege(client, campaignEntity.groupId,
                login, User.GroupLevelPrivilege.UPDATE_CAMPAIGN);

        //ensure there are no interactions for this campaign
        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.campaignIds.add(campaignId);

        int count = Interaction.count(client, qc);
        if (0 != count) {
            throw new APIException().status(APIResponse.Status.ERROR_PRECONDITION_FAILED)
                    .message("Found [" + count + "] interactions for campaign id [" + campaignId +
                            "]. Interactions should be deleted before deleting campaign");
        }

        ofy(client).delete().entity(campaignEntity).now();
    }

    public static List<CampaignEntity> query(String client, CampaignQueryCondition qc, String login) {
        Client.ensureValid(client);

        ensureNotNull(qc, "CampaignQueryCondition is null");

        User.ensureValidUser(client, login);

        Query<CampaignEntity> q = ofy(client).load().type(CampaignEntity.class);
        if (qc.enabled != null) {
            q = q.filter("enabled", qc.enabled);
        }

        if (qc.groupIds != null && !qc.groupIds.isEmpty()) {
            q = q.filter("groupId in", qc.groupIds);
        }

        if (qc.programIds != null && !qc.programIds.isEmpty()) {
            q = q.filter("programId in ", qc.programIds);
        }

        if (qc.endDateGreaterThanYYYYMMDD != null) {
            DateUtils.ensureFormatYYYYMMDD(qc.endDateGreaterThanYYYYMMDD);

            q = q.filter("endYYYYMMDD >", qc.endDateGreaterThanYYYYMMDD);
        }

        q = q.order("endYYYYMMDD");

        return q.list();
    }
}
