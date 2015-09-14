package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.api.endpoint.ProgramIshaApi.GroupEnum;
import crmdna.common.StopWatch;
import crmdna.common.Utils;
import crmdna.common.ValidationResultProp;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.common.contact.Contact;
import crmdna.common.contact.ContactProp;
import crmdna.email.EmailProp;
import crmdna.email.GAEEmail;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.gspreadsheet.GSpreadSheet;
import crmdna.list.ListProp;
import crmdna.mail2.MailMap;
import crmdna.mail2.MailMapFactory;
import crmdna.member.*;
import crmdna.user.User.GroupLevelPrivilege;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNullNotEmpty;

@Api(name = "mail")
public class ListApi {
    @ApiMethod(path = "createRestrictedList", httpMethod = HttpMethod.POST)
    public APIResponse createRestrictedList(@Named("client") ClientEnum clientEnum,
                                            @Named("group") GroupEnum groupEnum, @Named("displayName") String displayName,
                                            @Nullable @Named("practiceIds") Set<Long> practiceIds,
                                            @Nullable @Named("clientOther") String clientOther,
                                            @Nullable @Named("groupOtherIdOrName") String groupOtherIdOrName,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        long groupId = 0;

        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            groupId = EndpointUtils.getGroupId(client, groupEnum, groupOtherIdOrName);

            login = Utils.getLoginEmail(user);

            ListProp listProp =
                    crmdna.list.List.createRestricted(client, groupId, displayName, practiceIds, login);

            return new APIResponse().status(Status.SUCCESS).object(listProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "createPublicList", httpMethod = HttpMethod.POST)
    public APIResponse createPublicList(@Named("client") ClientEnum clientEnum,
                                        @Named("group") GroupEnum groupEnum, @Named("displayName") String displayName,
                                        @Nullable @Named("clientOther") String clientOther,
                                        @Nullable @Named("groupOtherIdOrName") String groupOtherIdOrName,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        long groupId = 0;

        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            groupId = EndpointUtils.getGroupId(client, groupEnum, groupOtherIdOrName);

            login = Utils.getLoginEmail(user);

            ListProp listProp = crmdna.list.List.createPublic(client, groupId, displayName, login);

            return new APIResponse().status(Status.SUCCESS).object(listProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "queryList", httpMethod = HttpMethod.GET)
    public APIResponse queryList(@Named("client") ClientEnum clientEnum,
                                 @Named("group") GroupEnum groupEnum, @Nullable @Named("clientOther") String clientOther,
                                 @Nullable @Named("groupOtherIdOrName") String groupOtherIdOrName,
                                 @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            long groupId = EndpointUtils.getGroupId(client, groupEnum, groupOtherIdOrName);

            List<ListProp> props = crmdna.list.List.querySortedProps(client, groupId);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "renameList", httpMethod = HttpMethod.GET)
    public APIResponse renameList(@Named("client") ClientEnum clientEnum,
                                  @Named("listId") long listId, @Named("newDisplayName") String newDisplayName,
                                  @Nullable @Named("clientOther") String clientOther,
                                  @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            ListProp listProp = crmdna.list.List.rename(client, listId, newDisplayName, login);

            return new APIResponse().status(Status.SUCCESS).object(listProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "deleteList", httpMethod = HttpMethod.GET)
    public APIResponse deleteList(@Named("client") ClientEnum clientEnum,
                                  @Named("listId") long listId, @Nullable @Named("clientOther") String clientOther,
                                  @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            crmdna.list.List.delete(client, listId, login);

            return new APIResponse().status(Status.SUCCESS).object("List [" + listId + "] deleted");
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "enableOrDisableList", httpMethod = HttpMethod.GET)
    public APIResponse enableOrDisableList(@Named("client") ClientEnum clientEnum,
                                           @Named("listId") long listId, @Named("enable") Boolean enable,
                                           @Nullable @Named("clientOther") String clientOther,
                                           @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);

            ListProp listProp = null;
            if (enable)
                listProp = crmdna.list.List.enable(client, listId, login);
            else
                listProp = crmdna.list.List.disable(client, listId, login);

            return new APIResponse().status(Status.SUCCESS).object(listProp);
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "subscribeOrUnsubscribe", httpMethod = HttpMethod.POST)
    public APIResponse subscribeOrUnsubscribe(@Named("client") ClientEnum clientEnum,
                                              @Nullable @Named("clientIfOther") String clientOther, @Named("listId") long listId,
                                              @Named("memberId") long memberId, @Named("action") SubscribeAction action,
                                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            if (action == SubscribeAction.SUBSCRIBE)
                Member.subscribeList_to_be_removed(client, memberId, listId, login);
            else
                Member.unsubscribeList_to_be_removed(client, memberId, listId, login);

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "subscribe", httpMethod = HttpMethod.POST)
    public APIResponse subscribe(@Named("client") ClientEnum clientEnum,
                                 @Nullable @Named("clientIfOther") String clientOther, @Named("listId") long listId,
                                 @Named("memberId") long memberId, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                 HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            Member.subscribeList_to_be_removed(client, memberId, listId, login);

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "subscribeEmail", httpMethod = HttpMethod.POST)
    public APIResponse subscribeEmail(@Named("client") ClientEnum clientEnum,
                                      @Nullable @Named("clientIfOther") String clientOther, @Named("group") GroupEnum groupEnum,
                                      @Named("list") ListEnum listEnum, @Named("email") String email,
                                      @Named("firstName") String firstName, @Named("lastName") String lastName,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            MailMap mailMap = new MailMap();
            mailMap.add(email, firstName, lastName);

            long groupId = Group.safeGetByIdOrName(client, groupEnum.toString()).toProp().groupId;

            long listId =
                    crmdna.list.List.safeGetByGroupIdAndName(client, groupId, listEnum.toString()).toProp().listId;

            BulkSubscriptionResultProp result = Member.bulkSubscribeList(client, listId, mailMap, login);

            return new APIResponse().status(Status.SUCCESS).object(result);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "bulkSubscribeFromPublishedGoogleSheet", httpMethod = HttpMethod.POST)
    public APIResponse bulkSubscribeFromPublishedGoogleSheet(@Named("client") ClientEnum clientEnum,
                                                             @Nullable @Named("clientIfOther") String clientOther, @Named("spreadSheetKey") String gsKey,
                                                             @Nullable @Named("numLinesToReadExclHeaderDefault4000") Integer numLinesExclHeader,
                                                             @Named("listId") long listId,
                                                             @Nullable @Named("firstNameIfMissingDefaultIsha") String defaultFirstName,
                                                             @Nullable @Named("lastNameIfMissingDefaultIsha") String defaultLastName,
                                                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        try {
            StopWatch stopWatch = StopWatch.createStarted();

            if (defaultFirstName == null)
                defaultFirstName = "Isha";

            if (defaultLastName == null)
                defaultLastName = "Isha";

            login = Utils.getLoginEmail(user);

            if (numLinesExclHeader == null)
                numLinesExclHeader = 4000;

            List<Map<String, String>> listOfMap =
                    GSpreadSheet.getPublishedSpreasheetAsListOfMap(gsKey, numLinesExclHeader);

            ensure(!listOfMap.isEmpty(), "No data found is spreadsheet");

            ValidationResultProp validationResultProp = MailApi.validate(listOfMap);
            if (validationResultProp.hasErrors())
                return new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT).object(
                        validationResultProp);

            ensureNotNullNotEmpty(defaultFirstName, "defaultFirstName not specified");
            ensureNotNullNotEmpty(defaultLastName, "defaultLastName not specified");

            MailMap mailMap = MailMapFactory.getFromListOfMap(listOfMap, defaultFirstName,
                    defaultLastName);

            BulkSubscriptionResultProp prop = Member.bulkSubscribeList(client, listId, mailMap, login);

            return new APIResponse().status(Status.SUCCESS).object(prop)
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "sendListAsCSV", httpMethod = HttpMethod.GET)
    public APIResponse sendListAsCSV(@Named("client") ClientEnum clientEnum,
                                     @Nullable @Named("clientIfOther") String clientOther, @Named("listId") long listId,
                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        try {
            StopWatch stopWatch = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);

            ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

            GroupProp groupProp = Group.safeGet(client, listProp.groupId).toProp();

            crmdna.user.User.ensureGroupLevelPrivilege(client, listProp.groupId, login,
                    GroupLevelPrivilege.VIEW_LIST);

            MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
            mqc.listIds.add(listId);
            mqc.subscribedGroupIds.add(listProp.groupId);

            List<MemberProp> memberProps = MemberLoader.queryProps(mqc, login);
            List<ContactProp> contactProps = new ArrayList<>();

            for (MemberProp memberProp : memberProps) {
                contactProps.add(memberProp.contact);
            }

            Collections.sort(contactProps);

            EmailProp emailProp = new EmailProp();

            String listQualifiedName = groupProp.displayName + "-" + listProp.displayName;
            emailProp.toEmailAddresses.add(login);
            emailProp.bodyHtml = "Members for list [" + listQualifiedName + "] attached";

            emailProp.csvAttachmentData = Contact.getCSV(contactProps);

            Format formatter = new SimpleDateFormat("dd-MMM-yy");
            String ddmmmyy = formatter.format(new Date());
            emailProp.attachmentName = listQualifiedName + " as of " + ddmmmyy + ".csv";

            emailProp.subject = "List [" + listQualifiedName + "] as of " + ddmmmyy;

            GAEEmail.send(emailProp);

            return new APIResponse().status(Status.SUCCESS).message("Email sent")
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    public enum ListEnum {
        SHAMBHAVI, MYSTIC_EYE, ISHA_KRIYA, IDY_SGP_2015, TEST, ISHA_UPA_YOGA, OTHER
    }

    public enum SubscribeAction {
        SUBSCRIBE, UNSUBSCRIBE
    }
}
