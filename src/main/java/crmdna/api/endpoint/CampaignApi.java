package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.calling.Campaign;
import crmdna.calling.CampaignEntity;
import crmdna.calling.CampaignProp;
import crmdna.calling.CampaignQueryCondition;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.program.Program;
import crmdna.program.ProgramProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Api(name = "interaction")
public class CampaignApi {
    @ApiMethod(path = "createCampaign", httpMethod = HttpMethod.POST)
    public APIResponse createCampaign(@Named("client") ClientApi.ClientEnum clientEnum,
                              @Nullable @Named("clientIfOther") String clientOther,
                              @Named("programId") long programId,
                              @Nullable @Named("startYYYYMMDDDefaultToday") Integer startDate,
                              @Nullable @Named("endYYYYMMDDDefaultProgramStart") Integer endDate,
                              @Nullable @Named("displayName") String displayName,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);

            if (startDate == null) {
                startDate = DateUtils.toYYYYMMDD(new Date());
            }

            ProgramProp programProp = Program.safeGet(client, programId).toProp(client);
            if (endDate == null) {
                endDate = programProp.startYYYYMMDD;
            }

            if (displayName == null) {
                displayName = programProp.getName();
            }

            login = Utils.getLoginEmail(user);
            CampaignProp campaignProp = Campaign.create(client, programId, displayName, startDate,
                    endDate, login);

            return new APIResponse().status(Status.SUCCESS).object(campaignProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getCampaign", httpMethod = HttpMethod.GET)
    public APIResponse getCampaign(@Named("client") ClientApi.ClientEnum clientEnum,
                                   @Nullable @Named("clientIfOther") String clientOther,
                                   @Named("campaignId") long campaignId,
                                   @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);

            CampaignProp campaignProp = Campaign.safeGet(client, campaignId).toProp();

            return new APIResponse().status(Status.SUCCESS).object(campaignProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "enableCampaign", httpMethod = HttpMethod.POST)
    public APIResponse enableCampaign(@Named("client") ClientApi.ClientEnum clientEnum,
                                      @Nullable @Named("clientIfOther") String clientOther,
                                      @Named("campaignId") long campaignId,
                                      @Named("enabled") boolean enable,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);


            CampaignProp campaignProp = Campaign.enable(client, campaignId, enable, login);

            return new APIResponse().status(Status.SUCCESS).object(campaignProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "renameCampaign", httpMethod = HttpMethod.POST)
    public APIResponse renameCampaign(@Named("client") ClientApi.ClientEnum clientEnum,
                                      @Nullable @Named("clientIfOther") String clientOther,
                                      @Named("campaignId") long campaignId,
                                      @Named("newName") String displayName,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);

            login = Utils.getLoginEmail(user);
            CampaignProp campaignProp = Campaign.rename(client, campaignId, displayName, login);

            return new APIResponse().status(Status.SUCCESS).object(campaignProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "updateDatesForCampaign", httpMethod = HttpMethod.POST)
    public APIResponse updateDatesForCampaign(@Named("client") ClientApi.ClientEnum clientEnum,
                                      @Nullable @Named("clientIfOther") String clientOther,
                                      @Named("campaignId") long campaignId,
                                      @Named("newStartYYYYMMDD") Integer newStartDate,
                                      @Named("newEndYYYYMMDD") Integer newEndDate,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            CampaignProp campaignProp = Campaign.updateDates(client, campaignId, newStartDate, newEndDate, login);

            return new APIResponse().status(Status.SUCCESS).object(campaignProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteCampaign", httpMethod = HttpMethod.POST)
    public APIResponse deleteCampaign(@Named("client") ClientApi.ClientEnum clientEnum,
                                              @Nullable @Named("clientIfOther") String clientOther,
                                              @Named("campaignId") long campaignId,
                                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            Campaign.delete(client, campaignId, login);

            return new APIResponse().status(Status.SUCCESS).message("Campaign [" + campaignId + "] deleted");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "queryCampaign", httpMethod = HttpMethod.GET)
    public APIResponse queryCampaign(@Named("client") ClientApi.ClientEnum clientEnum,
                                              @Nullable @Named("clientIfOther") String clientOther,
                                              @Nullable @Named("groupId") Long groupId,
                                              @Nullable @Named("programId") Long programId,
                                              @Nullable @Named("enabled") Boolean enabled,
                                              @Nullable @Named("endDateGreaterThanYYYYMMDD") Integer endDateGreaterThanYYYYMMDD,
                                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            CampaignQueryCondition qc = new CampaignQueryCondition();
            if (groupId != null) {
                qc.groupIds.add(groupId);
            }

            if (programId != null) {
                qc.programIds.add(programId);
            }

            if (enabled != null) {
                qc.enabled = enabled;
            }

            if (endDateGreaterThanYYYYMMDD != null) {
                qc.endDateGreaterThanYYYYMMDD = endDateGreaterThanYYYYMMDD;
            }

            List<CampaignEntity> entities = Campaign.query(client, qc, login);

            List<CampaignProp> props = new ArrayList<>();
            for (CampaignEntity e : entities) {
                props.add(e.toProp());
            }

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
