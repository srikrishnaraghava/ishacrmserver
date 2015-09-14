package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.member.Account;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.objectstore.ObjectStore;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static crmdna.common.AssertUtils.ensureNotNullNotEmpty;

@Api(name = "member")
public class MemberApi {
    @ApiMethod(path = "create", httpMethod = HttpMethod.POST)
    public APIResponse create(@Named("client") ClientEnum clientEnum,
                              @Nullable @Named("clientIfOther") String clientOther,
                              @Named("groupIdOrName") String groupIdOrName, @Nullable @Named("email") String email,
                              @Nullable @Named("firstName") String firstName, @Nullable @Named("lastName") String lastName,
                              @Nullable @Named("mobilePhone") String mobilePhone,
                              @Nullable @Named("homePhone") String homePhone,
                              @Nullable @Named("officePhone") String officePhone,
                              @Nullable @Named("homeAddress") String homeAddress,
                              @Nullable @Named("homeCity") String homeCity,
                              @Nullable @Named("homeCountry") String homeCountry,
                              @Nullable @Named("homePincode") String homePincode,
                              @Nullable @Named("occupation") String occupation,
                              @Nullable @Named("company") String company,
                              @Nullable @Named("officeAddress") String officeAddress,
                              @Nullable @Named("officePincode") String officePincode,
                              @Nullable @Named("allowDuplicateEmail") Boolean allowDuplicateEmail,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            ContactProp contact = new ContactProp();
            contact.email = email;
            contact.firstName = firstName;
            contact.lastName = lastName;
            contact.mobilePhone = mobilePhone;
            contact.homePhone = homePhone;
            contact.officePhone = officePhone;
            contact.homeAddress.address = homeAddress;
            contact.homeAddress.city = homeCity;
            contact.homeAddress.country = homeCountry;
            contact.homeAddress.pincode = homePincode;
            contact.occupation = occupation;
            contact.company = company;
            contact.officeAddress.address = officeAddress;
            contact.officeAddress.pincode = officePincode;
            contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

            if (allowDuplicateEmail == null)
                allowDuplicateEmail = false;

            login = Utils.getLoginEmail(user);

            long groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;
            MemberProp memberProp = Member.create(client, groupId, contact, allowDuplicateEmail, login);

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "updateContactDetails", httpMethod = HttpMethod.POST)
    public APIResponse updateContactDetails(@Named("client") ClientEnum clientEnum,
                                            @Nullable @Named("clientIfOther") String clientOther, @Named("memberId") long memberId,
                                            @Nullable @Named("email") String email, @Nullable @Named("firstName") String firstName,
                                            @Nullable @Named("lastName") String lastName,
                                            @Nullable @Named("mobilePhone") String mobilePhone,
                                            @Nullable @Named("homePhone") String homePhone,
                                            @Nullable @Named("officePhone") String officePhone,
                                            @Nullable @Named("homeAddress") String homeAddress,
                                            @Nullable @Named("homeCity") String homeCity, @Nullable @Named("homeState") String homeState,
                                            @Nullable @Named("homeCountry") String homeCountry,
                                            @Nullable @Named("homePincode") String homePincode,
                                            @Nullable @Named("occupation") String occupation,
                                            @Nullable @Named("company") String company,
                                            @Nullable @Named("officeAddress") String officeAddress,
                                            @Nullable @Named("officePincode") String officePincode,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            ContactProp contact = new ContactProp();
            contact.email = email;
            contact.firstName = firstName;
            contact.lastName = lastName;
            contact.mobilePhone = mobilePhone;
            contact.homePhone = homePhone;
            contact.officePhone = officePhone;
            contact.homeAddress.address = homeAddress;
            contact.homeAddress.city = homeCity;
            contact.homeAddress.state = homeState;
            contact.homeAddress.country = homeCountry;
            contact.homeAddress.pincode = homePincode;
            contact.occupation = occupation;
            contact.company = company;
            contact.officeAddress.address = officeAddress;
            contact.officeAddress.pincode = officePincode;
            contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

            login = Utils.getLoginEmail(user);
            MemberProp memberProp = Member.updateContactDetails(client, memberId, contact, login);

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "get", httpMethod = HttpMethod.GET)
    public APIResponse get(@Named("client") ClientEnum clientEnum,
                           @Nullable @Named("clientIfOther") String clientOther, @Named("memberId") long memberId,
                           @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            MemberProp memberProp = MemberLoader.safeGet(client, memberId, login).toProp();

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "query", httpMethod = HttpMethod.GET)
    public APIResponse query(@Named("client") ClientEnum clientEnum,
                             @Nullable @Named("clientIfOther") String clientOther,
                             @Nullable @Named("searchStr") String searchStr,
                             @Nullable @Named("groupIds") Set<Long> groupIds,
                             @Nullable @Named("practiceIds") Set<Long> practiceIds,
                             @Nullable @Named("programIds") Set<Long> programIds,
                             @Nullable @Named("listIds") Set<Long> listIds,
                             @Nullable @Named("subscribedGroupIds") Set<Long> subscribedGroupIds,
                             @Nullable @Named("unsubscribedGroupIds") Set<Long> unsubscribedGroupIds,
                             @Nullable @Named("maxResultSizeDefault10") Integer maxResultSize,
                             @Nullable @Named("sendEmailDefaultFalse") Boolean sendEmail,
                             @Nullable @Named("emailAttachmentName") String emailAttachmentName,
                             @Nullable @Named("nameFirstChar") Utils.SingleChar nameFirstChar,
                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;

        String login = null;

        try {
            if (maxResultSize == null)
                maxResultSize = 10;

            client = EndpointUtils.getClient(clientEnum, clientOther);

            MemberQueryCondition qc = new MemberQueryCondition(client, maxResultSize);
            qc.searchStr = searchStr;
            qc.groupIds = groupIds;
            qc.practiceIds = practiceIds;
            qc.programIds = programIds;
            qc.subscribedGroupIds = subscribedGroupIds;
            qc.unsubscribedGroupIds = unsubscribedGroupIds;
            qc.listIds = listIds;

            if (nameFirstChar != null) {
                qc.nameFirstChar = nameFirstChar.toString();
            }

            login = Utils.getLoginEmail(user);
            crmdna.user.User.ensureValidUser(client, login);

            if (sendEmail == null)
                sendEmail = false;

            if (sendEmail) {
                //process asynchronously using task queue
                ensureNotNullNotEmpty(emailAttachmentName, "emailAttachmentName not specified");

                long tempAccessId = ObjectStore.put(client, "temp access id", 30, ObjectStore.TimeUnit.SECONDS);

                Queue queue = QueueFactory.getDefaultQueue();
                queue.add(TaskOptions.Builder.withUrl("/memberReport/sendReportAsEmail")
                    .param("qc", new Gson().toJson(qc))
                    .param("email", login)
                    .param("emailAttachmentName", emailAttachmentName)
                    .param("accessId", tempAccessId + ""));

                return new APIResponse().status(Status.ASYNC_CALL_SUBMITTED).message("This report has been submitted for processing. " +
                        " Once complete the results will be emailed to [" + login + "]");
            } else {
                List<MemberProp> memberProps = MemberLoader.querySortedProps(qc, login);

                return new APIResponse().status(Status.SUCCESS).object(memberProps)
                        .message("No of records: " + memberProps.size());
            }
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "quickSearch", httpMethod = HttpMethod.GET)
    public APIResponse quickSearch(@Named("client") ClientEnum clientEnum,
                                   @Nullable @Named("clientIfOther") String clientOther,
                                   @Nullable @Named("searchStr") String searchStr,
                                   @Nullable @Named("groupIds") Set<Long> groupIds,
                                   @Nullable @Named("maxResultSizeDefault10") Integer maxResultSize,
                                   @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        if (maxResultSize == null)
            maxResultSize = 10;

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            List<MemberProp> memberProps =
                    MemberLoader.quickSearch(client, searchStr, groupIds, maxResultSize, login);

            return new APIResponse().status(Status.SUCCESS).object(memberProps)
                    .message("No of records: " + memberProps.size());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getDetailedInfo", httpMethod = HttpMethod.GET)
    public APIResponse getDetailedInfo(@Named("client") ClientEnum clientEnum,
                                       @Nullable @Named("clientIfOther") String clientOther, @Named("memberId") long memberId,
                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            MemberProp memberProp = MemberLoader.safeGetDetailedInfo(client, memberId, login);

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addOrDeleteProgram", httpMethod = HttpMethod.POST)
    public APIResponse addOrDeleteProgram(@Named("client") ClientEnum clientEnum,
                                          @Nullable @Named("clientIfOther") String clientOther, @Named("memberId") long memberId,
                                          @Named("programId") long programId, @Named("add") Boolean add,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            MemberProp memberProp = Member.addOrDeleteProgram(client, memberId, programId, add, login);

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addOrDeleteGroup", httpMethod = HttpMethod.POST)
    public APIResponse addOrDeleteGroup(@Named("client") ClientEnum clientEnum,
                                        @Nullable @Named("clientIfOther") String clientOther, @Named("memberId") long memberId,
                                        @Named("groupId") long groupId, @Named("add") Boolean add,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            MemberProp memberProp = Member.addOrDeleteGroup(client, memberId, groupId, add, login);

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addOrDeleteList", httpMethod = HttpMethod.POST)
    public APIResponse addOrDeleteList(@Named("client") ClientEnum clientEnum,
                                        @Nullable @Named("clientIfOther") String clientOther, @Named("memberId") long memberId,
                                        @Named("listId") long listId, @Named("add") Boolean add,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            boolean changed = Member.addOrDeleteList(client, memberId, listId, add, login);

            MemberProp memberProp = MemberLoader.safeGet(client, memberId, login).toProp();

            return new APIResponse().status(Status.SUCCESS).object(memberProp).message("Changed: " + changed);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "subscribeOrUnsubscribeGroup", httpMethod = HttpMethod.POST)
    public APIResponse subscribeOrUnsubscribeGroup(@Named("clientDropdown") ClientEnum clientEnum,
                                        @Nullable @Named("clientIfOther") String clientOther, @Named("memberId") long memberId,
                                        @Nullable @Named("groupDropdown") ProgramIshaApi.GroupEnum groupEnum,
                                        @Nullable @Named("groupIdOrName") String groupIdOrName,
                                        @Named("subscribe") Boolean subscribe,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            long groupId = EndpointUtils.getGroupId(client, groupEnum, groupIdOrName);

            login = Utils.getLoginEmail(user);

            if (subscribe) {
                Member.subscribeGroup(client, memberId, groupId, login);
            } else {
                Member.unsubscribeGroup(client, memberId, groupId, login);
            }

            MemberProp memberProp = MemberLoader.safeGet(client, memberId,  login).toProp();

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getUnsubscribedEmails", httpMethod = HttpMethod.GET)
    public APIResponse getUnsubscribedEmails(@Named("clientDropdown") ClientEnum clientEnum,
                                                   @Nullable @Named("clientIfOther") String clientOther,
                                                   @Nullable @Named("groupDropdown") ProgramIshaApi.GroupEnum groupEnum,
                                                   @Nullable @Named("groupIdOrName") String groupIdOrName,
                                                   @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            long groupId = EndpointUtils.getGroupId(client, groupEnum, groupIdOrName);

            login = Utils.getLoginEmail(user);

            TreeSet<String> unsubscribedEmails = MemberLoader.getUnsubscribedEmails(client, groupId, login);

            return new APIResponse().status(Status.SUCCESS).object(unsubscribedEmails)
                    .message("No of unsubscribed emails: " + unsubscribedEmails.size());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "verifyMember", httpMethod = HttpMethod.POST)
    public APIResponse verifyMember(@Named("client") ClientEnum clientEnum,
                                        @Nullable @Named("clientIfOther") String clientOther, @Named("memberEmail") String memberEmail,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            MemberProp memberProp = MemberLoader.safeGetByIdOrEmail(client, memberEmail, login).toProp();

            return new APIResponse().status(Status.SUCCESS).object(
                    Account.verifyEmail(client, memberProp.memberId, memberProp.verificationCode));

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
