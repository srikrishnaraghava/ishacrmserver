package crmdna.api.servlet;

import com.google.gson.Gson;
import crmdna.common.AssertUtils;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.common.contact.ContactProp;
import crmdna.email.EmailProp;
import crmdna.email.GAEEmail;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.member.MemberUtils;
import crmdna.objectstore.ObjectStore;
import crmdna.program.Program;
import crmdna.program.ProgramProp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static crmdna.common.AssertUtils.ensureNotNull;

public class MemberServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private void quickSearch(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        List<MemberProp> memberProps =
            MemberLoader.quickSearch(client, request.getParameter("searchStr"), null, 10, login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(memberProps)
            .message("No of records: " + memberProps.size()));
    }

    private void detailedInfo(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        MemberProp memberProp = MemberLoader
            .safeGetDetailedInfo(client, ServletUtils.getLongParam(request, "memberId"), login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(memberProp));
    }

    private void create(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        ContactProp contact = new ContactProp();
        contact.email = ServletUtils.getStrParam(request, "email");
        contact.firstName = ServletUtils.getStrParam(request, "firstName");
        contact.lastName = ServletUtils.getStrParam(request, "lastName");
        contact.mobilePhone = ServletUtils.getStrParam(request, "mobilePhone");
        contact.homePhone = ServletUtils.getStrParam(request, "homePhone");
        contact.officePhone = ServletUtils.getStrParam(request, "officePhone");
        contact.homeAddress.address = ServletUtils.getStrParam(request, "homeAddress.address");
        contact.homeAddress.city = ServletUtils.getStrParam(request, "homeAddress.city");
        contact.homeAddress.state = ServletUtils.getStrParam(request, "homeAddress.state");
        contact.homeAddress.country = ServletUtils.getStrParam(request, "homeAddress.country");
        contact.homeAddress.pincode = ServletUtils.getStrParam(request, "homeAddress.pincode");
        contact.occupation = ServletUtils.getStrParam(request, "occupation");
        contact.company = ServletUtils.getStrParam(request, "company");
        contact.officeAddress.address = ServletUtils.getStrParam(request, "officeAddress.address");
        contact.officeAddress.pincode = ServletUtils.getStrParam(request, "officeAddress.pincode");
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

        Set<Long> groupIds = ServletUtils.getLongParamsAsSet(request, "groupIds");
        AssertUtils.ensureNotNull(groupIds, "GroupIds cannot be null");
        MemberProp memberProp =
            Member.create(client, groupIds.iterator().next(), contact, false, login);
        for (Long groupId : groupIds) {
            Member.addOrDeleteGroup(client, memberProp.memberId, groupId, true, login);
        }

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(memberProp));
    }

    private void updateContactDetails(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        ContactProp contact = new ContactProp();
        contact.email = ServletUtils.getStrParam(request, "email");
        contact.firstName = ServletUtils.getStrParam(request, "firstName");
        contact.lastName = ServletUtils.getStrParam(request, "lastName");
        contact.mobilePhone = ServletUtils.getStrParam(request, "mobilePhone");
        contact.homePhone = ServletUtils.getStrParam(request, "homePhone");
        contact.officePhone = ServletUtils.getStrParam(request, "officePhone");
        contact.homeAddress.address = ServletUtils.getStrParam(request, "homeAddress.address");
        contact.homeAddress.city = ServletUtils.getStrParam(request, "homeAddress.city");
        contact.homeAddress.state = ServletUtils.getStrParam(request, "homeAddress.state");
        contact.homeAddress.country = ServletUtils.getStrParam(request, "homeAddress.country");
        contact.homeAddress.pincode = ServletUtils.getStrParam(request, "homeAddress.pincode");
        contact.occupation = ServletUtils.getStrParam(request, "occupation");
        contact.company = ServletUtils.getStrParam(request, "company");
        contact.officeAddress.address = ServletUtils.getStrParam(request, "officeAddress.address");
        contact.officeAddress.pincode = ServletUtils.getStrParam(request, "officeAddress.pincode");
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

        MemberProp memberProp = Member
            .updateContactDetails(client, ServletUtils.getLongParam(request, "memberId"), contact,
                login);

        Set<Long> groupIds = ServletUtils.getLongParamsAsSet(request, "groupIds");
        AssertUtils.ensureNotNull(groupIds, "GroupIds cannot be null");
        for (Long groupId : groupIds) {
            if (!memberProp.groupIds.contains(groupId)) {
                Member.addOrDeleteGroup(client, memberProp.memberId, groupId, true, login);
            }
        }

        for (long groupId : memberProp.groupIds) {
            if (!groupIds.contains(groupId)) {
                Member.addOrDeleteGroup(client, memberProp.memberId, groupId, false, login);
            }
        }

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(memberProp));
    }

    private void addUnverifiedProgram(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        int yyyymmdd = ServletUtils.getIntParam(request, "date");
        DateUtils.Month month = DateUtils.getMonthEnum(yyyymmdd);

        Member.addUnverifiedProgram(client, ServletUtils.getLongParam(request, "memberId"),
            ServletUtils.getLongParam(request, "programTypeId"), month, yyyymmdd / 10000,
            ServletUtils.getStrParam(request, "city"), ServletUtils.getStrParam(request, "country"),
            ServletUtils.getStrParam(request, "teacher"), login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
    }

    private void removeUnverifiedProgram(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        Member.deleteUnverifiedProgram(client, ServletUtils.getLongParam(request, "memberId"),
            ServletUtils.getIntParam(request, "unverifiedProgramId"), login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
    }

    private void updateSubscription(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        long memberId = ServletUtils.getLongParam(request, "memberId");
        MemberProp memberProp = MemberLoader.safeGet(client, memberId, login).toProp();

        Set<Long> subscribeToGroupIds =
            ServletUtils.getLongParamsAsSet(request, "subscribeToGroupIds");
        for (long groupId : Utils.safe(subscribeToGroupIds)) {
            Member.subscribeGroup(client, memberId, groupId, login);
        }

        Set<Long> unsubscribeToGroupIds =
            ServletUtils.getLongParamsAsSet(request, "unsubscribeToGroupIds");
        for (long groupId : Utils.safe(unsubscribeToGroupIds)) {
            Member.unsubscribeGroup(client, memberId, groupId, login);
        }

        Set<Long> addListIds = ServletUtils.getLongParamsAsSet(request, "addListIds");
        for (long listId : Utils.safe(addListIds)) {
            Member.addOrDeleteList(client, memberProp.memberId, listId, true, login);
        }

        Set<Long> removeListIds = ServletUtils.getLongParamsAsSet(request, "removeListIds");
        for (long listId : Utils.safe(removeListIds)) {
            Member.addOrDeleteList(client, memberProp.memberId, listId, false, login);
        }

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
    }

    private void query(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Integer maxResultSize = ServletUtils.getIntParam(request, "maxResultSize");
        boolean sendAsAttachment = ServletUtils.getBoolParam(request, "sendAsAttachment");

        MemberQueryCondition qc = new MemberQueryCondition(client, maxResultSize);
        qc.programTypeIds = ServletUtils.getLongParamsAsSet(request, "programTypeIds");

        Integer startYYYYMMDD = ServletUtils.getIntParam(request, "startYYYYMMDD");
        Integer endYYYYMMDD = ServletUtils.getIntParam(request, "endYYYYMMDD");
        if ((qc.programTypeIds != null) || (startYYYYMMDD != null) || (endYYYYMMDD != null)) {
            List<ProgramProp> programProps = Program
                .query(client, startYYYYMMDD, endYYYYMMDD, qc.programTypeIds, null, null, 1000);
            for (ProgramProp programProp : programProps) {
                qc.programIds.add(programProp.programId);
            }
        }

        qc.practiceIds = ServletUtils.getLongParamsAsSet(request, "practiceIds");
        qc.occupations = ServletUtils.getLongParamsAsSet(request, "occupations");

        int resultSize = MemberLoader.getCount(qc, login);
        if (sendAsAttachment) {
            qc.maxResultSize = MemberLoader.MAX_RESULT_SIZE;
            MemberUtils.queryAsync(qc, login, null, null, resultSize);
            ServletUtils.setJson(response, new APIResponse().status(Status.ASYNC_CALL_SUBMITTED)
                .message("Results will be emailed to " + login));
        } else {
            if (resultSize > MemberLoader.MAX_RESULT_SIZE) {
                ServletUtils.setJson(response,
                    new APIResponse().object(resultSize).status(Status.ERROR_OVERFLOW));
            } else {
                List<MemberProp> memberProps = MemberLoader.querySortedProps(qc, login);
                for (MemberProp memberProp : memberProps) {
                    MemberLoader.populateDependents(client, memberProp, login);
                }
                ServletUtils.setJson(response,
                    new APIResponse().object(memberProps).status(Status.SUCCESS));
            }
        }
    }

    private void sendReportAsEmail(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        String queryCondition = ServletUtils.getStrParam(request, "qc");
        AssertUtils.ensureNotNullNotEmpty(queryCondition, "Parameter [qc] is missing");

        try {

            MemberQueryCondition mqc = new Gson().fromJson(queryCondition, MemberQueryCondition.class);

            if (client == null)
                client = mqc.client;

            String accessId = ServletUtils.getStrParam(request, "accessId");
            AssertUtils.ensureNotNullNotEmpty(accessId, "Parameter [accessId] is missing");

            String emailAttachmentName = ServletUtils.getStrParam(request, "emailAttachmentName");
            AssertUtils
                .ensureNotNullNotEmpty(emailAttachmentName, "emailAttachmentName is null or empty");

            Object o = ObjectStore.get(client, Long.parseLong(accessId));
            ensureNotNull(o, "Invalid access id");

            login = ServletUtils.getStrParam(request, "email");
            AssertUtils.ensureNotNullNotEmpty(login, "Parameter [email] is missing");
            Utils.ensureValidEmail(login);

            Integer partNumber = ServletUtils.getIntParam(request, "partNumber");
            Integer totalParts = ServletUtils.getIntParam(request, "totalParts");
            String partStr = partNumber + "/" + totalParts;

            List<MemberProp> memberProps = MemberLoader.queryWithCursor(mqc, login);

            if (memberProps.size() > 0) {
                EmailProp emailProp = new EmailProp();
                emailProp.toEmailAddresses.add(login);
                emailProp.bodyHtml = "<br>Query results attached";
                emailProp.csvAttachmentData = Member.getCSV(client, memberProps);
                emailProp.attachmentName = emailAttachmentName + "- [" + partStr + "].csv";
                emailProp.subject = "Member Query Results [" + partStr + "]";
                GAEEmail.send(emailProp);

                if (mqc.cursor != null) {
                    MemberUtils.queryAsync(mqc, login, partNumber + 1, totalParts, null);
                }
            }
            ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils
                .setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            String login = ServletUtils.getLogin(request);

            try {
                switch (action) {
                    case "quickSearch":
                        quickSearch(client, login, request, response);
                        break;
                    case "detailedInfo":
                        detailedInfo(client, login, request, response);
                        break;
                    case "create":
                        create(client, login, request, response);
                        break;
                    case "updateContactDetails":
                        updateContactDetails(client, login, request, response);
                        break;
                    case "addUnverifiedProgram":
                        addUnverifiedProgram(client, login, request, response);
                        break;
                    case "removeUnverifiedProgram":
                        removeUnverifiedProgram(client, login, request, response);
                        break;
                    case "updateSubscription":
                        updateSubscription(client, login, request, response);
                        break;
                    case "query":
                        query(client, login, request, response);
                        break;
                    case "sendReportAsEmail":
                        sendReportAsEmail(client, login, request, response);
                        break;
                    default:
                        ServletUtils.setJson(response,
                            new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }
            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true,
                    new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils
                .setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            try {
                switch (action) {
                    default:
                        ServletUtils.setJson(response,
                            new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }
            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils
                    .toAPIResponse(ex, true, new RequestInfo().client(client).req(request)));
            }
        }
    }
}
