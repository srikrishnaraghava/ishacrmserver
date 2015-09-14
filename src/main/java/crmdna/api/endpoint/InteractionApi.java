package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.common.DateUtils;
import crmdna.common.DateUtils.DateRange;
import crmdna.common.StopWatch;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.interaction.Interaction;
import crmdna.interaction.Interaction.InteractionType;
import crmdna.interaction.InteractionProp;
import crmdna.interaction.InteractionQueryCondition;
import crmdna.interaction.InteractionQueryResult;
import crmdna.interaction.InteractionScore;
import crmdna.interaction.InteractionScoreProp;
import crmdna.interaction.UserMemberProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Api(name = "interaction")
public class InteractionApi {
    @ApiMethod(path = "createInteraction", httpMethod = HttpMethod.POST)
    public APIResponse createInteraction(@Named("client") String client,
                                         @Named("memberId") long memberId, @Named("interactionType") InteractionType interactionType,
                                         @Named("content") String content,
                                         @Nullable @Named("campaignId") Long campaignId,
                                         @Nullable @Named("ensureWithinCampaignDatesDefaultTrue") Boolean ensureWithinCampaignDates,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                         HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);

            ensureWithinCampaignDates = (ensureWithinCampaignDates == null) ? true : ensureWithinCampaignDates;
            InteractionProp prop =
                    Interaction.createInteraction(client, memberId, content, interactionType, new Date(),
                            campaignId, ensureWithinCampaignDates, login);

            return new APIResponse().status(Status.SUCCESS).object(prop)
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "updateInteraction", httpMethod = HttpMethod.POST)
    public APIResponse updateInteraction(@Named("client") String client,
                                         @Named("interactionId") long interactionId, @Nullable @Named("newMemberId") Long newMemberId,
                                         @Nullable @Named("newInteractionType") InteractionType newInteractionType,
                                         @Nullable @Named("newUserEmail") String newUserEmail,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);

            InteractionProp prop =
                    Interaction.updateInteraction(client, interactionId, newMemberId, newInteractionType,
                            newUserEmail, login);

            return new APIResponse().status(Status.SUCCESS).object(prop)
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteInteraction", httpMethod = HttpMethod.DELETE)
    public APIResponse deleteInteraction(@Named("client") String client,
                                         @Named("interactionId") long interactionId,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();
            login = Utils.getLoginEmail(user);

            Interaction.deleteInteraction(client, interactionId, login);

            return new APIResponse().status(Status.SUCCESS)
                    .object("interaction Id [" + interactionId + "] deleted")
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "query", httpMethod = HttpMethod.GET)
    public APIResponse query(@Named("client") String client,
                             @Nullable @Named("memberId") Long memberId, @Nullable @Named("userId") Long userId,
                             @Nullable @Named("campaignId") Long campaignId,
                             @Nullable @Named("interactionType") InteractionType interactionType,
                             @Nullable @Named("dateRange") DateRange dateRange,
                             @Nullable @Named("startIndex") Integer startIndex,
                             @Nullable @Named("numResults") Integer numResults,
                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();
            login = Utils.getLoginEmail(user);

            InteractionQueryCondition qc = new InteractionQueryCondition();
            if (memberId != null) {
                qc.memberIds.add(memberId);
            }
            if (userId != null) {
                qc.userIds.add(userId);
            }
            if (null != interactionType) {
                qc.interactionTypes.add(interactionType.toString());
            }
            if (null != campaignId) {
                qc.campaignIds.add(campaignId);
            }
            qc.end = new Date();
            if (dateRange != null) {
                qc.start = new Date(qc.end.getTime() - DateUtils.getMilliSecondsFromDateRange(dateRange));
            }

            qc.numResults = numResults;

            InteractionQueryResult result =
                    Interaction.query(client, qc, login);

            return new APIResponse().status(Status.SUCCESS).object(result)
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "createSubInteraction", httpMethod = HttpMethod.POST)
    public APIResponse createSubInteraction(@Named("client") String client,
                                            @Named("interactionId") long interactionId, @Named("content") String content,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();
            login = Utils.getLoginEmail(user);

            Interaction.createSubInteraction(client, interactionId, content, new Date(), login);

            return new APIResponse().status(Status.SUCCESS).message("Added sub interaction")
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteSubInteraction", httpMethod = HttpMethod.DELETE)
    public APIResponse deleteSubInteraction(@Named("client") String client,
                                            @Named("interactionId") long interactionId, @Named("subInteractionId") long subInteractionId,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);

            Interaction.deleteSubInteraction(client, interactionId, subInteractionId, login);

            return new APIResponse().status(Status.SUCCESS)
                    .message("Sub interaction [" + subInteractionId + "] deleted")
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "updateSubInteraction", httpMethod = HttpMethod.POST)
    public APIResponse updateSubInteraction(@Named("client") String client,
                                            @Named("interactionId") long interactionId, @Named("subInteractionId") long subInteractionId,
                                            @Named("content") String content, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                            HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            StopWatch sw = StopWatch.createStarted();
            login = Utils.getLoginEmail(user);

            Interaction.updateSubInteraction(client, interactionId, subInteractionId, content, login);

            return new APIResponse().status(Status.SUCCESS)
                    .message("Sub interaction [" + subInteractionId + "] updated")
                    .processingTimeInMS(sw.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getInteractionScore", httpMethod = HttpMethod.GET)
    public APIResponse getInteractionScore(@Named("client") ClientApi.ClientEnum clientEnum,
                                            @Nullable @Named("clientIfOther") String clientOther,
                                            @Named("memberId") long memberId, @Named("userId") long userId,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                            HttpServletRequest req) {

        String client = null;
        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);

            UserMemberProp userMember = new UserMemberProp();
            userMember.memberId = memberId;
            userMember.userId = userId;

            List<InteractionScoreProp> props = InteractionScore.get(client, Utils.getList(userMember));

            return new APIResponse().status(Status.SUCCESS)
                    .object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }
}
