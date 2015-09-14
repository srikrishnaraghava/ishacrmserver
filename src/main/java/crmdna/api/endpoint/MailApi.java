package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.api.endpoint.ProgramIshaApi.GroupEnum;
import crmdna.common.DateUtils;
import crmdna.common.DateUtils.DateRange;
import crmdna.common.StopWatch;
import crmdna.common.Utils;
import crmdna.common.ValidationResultProp;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.email.EmailProp;
import crmdna.email.GAEEmail;
import crmdna.group.Group;
import crmdna.gspreadsheet.GSpreadSheet;
import crmdna.mail2.*;
import crmdna.user.UserCore;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static crmdna.common.AssertUtils.*;

@Api(name = "mail")
public class MailApi {

    static ValidationResultProp validate(List<Map<String, String>> listOfMap) {

        ValidationResultProp validationResultProp = new ValidationResultProp();

        if (listOfMap.isEmpty()) {
            return validationResultProp;
        }

        Map<String, String> map = listOfMap.get(0);
        if (!map.containsKey("firstname")) {
            String message = "Column [First Name] (space and case insensitive) is missing";
            validationResultProp.errors.add(message);

            return validationResultProp;
        }

        if (!map.containsKey("lastname")) {
            String message = "Column [Last Name] (space and case insensitive) is missing";
            validationResultProp.errors.add(message);

            return validationResultProp;
        }

        if (!map.containsKey("email")) {
            String message = "Column [Email] (case insensitive) is missing";
            validationResultProp.errors.add(message);

            return validationResultProp;
        }

        List<String> errors = new ArrayList<>();
        for (int i = 0; i < listOfMap.size(); i++) {
            map = listOfMap.get(i);

            ensure(map.containsKey("firstname"));
            ensure(map.containsKey("lastname"));
            ensure(map.containsKey("email"));


            String email = map.get("email").replace(" ", "");
            if (!Utils.isValidEmailAddress(email))
                errors.add("Error in line [" + (i + 2) + "]: Email [" + email + "] is invalid");
        }

        validationResultProp.errors = errors;
        validationResultProp.numEntries = listOfMap.size();

        return validationResultProp;
    }

    @ApiMethod(path = "sendGAEDummyEmailToLoggedInUser", httpMethod = HttpMethod.POST)
    public APIResponse sendGAEDummyEmailToLoggedInUser(@Named("client") String client,
                                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            if (null == user)
                throw new APIException().status(Status.ERROR_LOGIN_REQUIRED).message("Please log in");

            login = user.getEmail();

            EmailProp emailProp = new EmailProp();
            emailProp.attachmentName = "test.csv";
            emailProp.bodyHtml = "<h3>Sample html content </h3>";
            emailProp.subject = "Test subject";
            emailProp.toEmailAddresses.add(login);
            emailProp.csvAttachmentData =
                    "first name, last name\nfirst name, last name\nfirst name, last name\n";

            GAEEmail.send(emailProp);

            return new APIResponse().status(Status.SUCCESS).object("Sent dummy email to " + login);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "sendGAETestAlertToDevTeam", httpMethod = HttpMethod.POST)
    public APIResponse sendGAETestAlertToDevTeam(
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            if (null == login)
                throw new APIException().status(Status.ERROR_LOGIN_REQUIRED).message(
                        "Please log in as supper user");

            if (!UserCore.isSuperUser(login))
                throw new APIException().status(Status.ERROR_INSUFFICIENT_PERMISSION).message(
                        "This call is only allowed for super user");

            Utils.sendAlertEmailToDevTeam("TestClient", new RuntimeException(
                    "Test exception: please ignore"), req, login);

            return new APIResponse().status(Status.SUCCESS).object("Success");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login));
        }
    }

    @ApiMethod(path = "createMailContent", httpMethod = HttpMethod.POST)
    public APIResponse createMailContent(@Named("client") String client,
                                         @Named("displayName") String displayName, @Named("groupDropDown") GroupEnum groupDropDown,
                                         @Nullable @Named("groupOtherIdOrName") String groupIdOrName,
                                         @Named("subject") String subject, @Nullable @Named("bodyUrl") String bodyUrl,
                                         @Nullable @Named("bodyHtml") String bodyHtml,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (null == client)
            client = "isha";

        try {

            login = Utils.getLoginEmail(user);

            if (groupDropDown == GroupEnum.OTHER)
                ensure(groupIdOrName != null,
                        "groupOtherIdOrName should be specified with groupDropDown is 'OTHER'");

            if (groupDropDown != GroupEnum.OTHER)
                groupIdOrName = groupDropDown.toString();

            long groupId;
            if (groupIdOrName.equals("0"))
                groupId = 0;
            else
                groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;

            ensure((bodyHtml != null) ^ (bodyUrl != null),
                    "Either bodyHtml or bodyUrl should be specified");

            if (bodyUrl != null)
                bodyHtml = Utils.readDataFromURL(bodyUrl);

            MailContentProp mailContentProp =
                    MailContent.create(client, displayName, groupId, subject, bodyHtml, login);

            mailContentProp.bodyUrl =
                    req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                            + "/mailContent/get?client=" + client + "&mailContentId="
                            + mailContentProp.mailContentId;

            return new APIResponse().status(Status.SUCCESS).object(mailContentProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "updateMailContent", httpMethod = HttpMethod.POST)
    public APIResponse updateMailContent(@Named("client") String client,
                                         @Named("mailContentId") long mailContentId,
                                         @Nullable @Named("newDisplayName") String newDisplayName,
                                         @Nullable @Named("newSubject") String newSubject,
                                         @Nullable @Named("newBodyUrl") String newBodyUrl,
                                         @Nullable @Named("newBodyHtml") String newBodyHtml,
                                         @Nullable @Named("allowIfMailsSentDefaultFalse") Boolean allowUpdateIfMailsSent,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (null == client)
            client = "isha";

        try {

            if (allowUpdateIfMailsSent == null)
                allowUpdateIfMailsSent = false;

            login = Utils.getLoginEmail(user);

            ensure((newBodyHtml == null) || (newBodyUrl == null),
                    "Both newBodyHtml and newBodyUrl cannot be specified");

            if (newBodyUrl != null)
                newBodyHtml = Utils.readDataFromURL(newBodyUrl);

            MailContentProp mailContentProp =
                    MailContent.update(client, mailContentId, newDisplayName, newSubject, newBodyHtml,
                            allowUpdateIfMailsSent, login);

            mailContentProp.bodyUrl =
                    req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                            + "/mailContent/get?client=" + client + "&mailContentId="
                            + mailContentProp.mailContentId;

            return new APIResponse().status(Status.SUCCESS).object(mailContentProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "deleteMailContent", httpMethod = HttpMethod.POST)
    public APIResponse deleteMailContent(@Named("client") String client,
                                         @Named("mailContentId") long mailContentId,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (null == client)
            client = "isha";

        try {

            login = Utils.getLoginEmail(user);

            MailContent.delete(client, mailContentId, login);

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "queryMailContent", httpMethod = HttpMethod.GET)
    public APIResponse queryMailContent(@Named("client") String client,
                                        @Nullable @Named("owner") String owner, @Nullable @Named("dateRange") DateRange dateRange,
                                        @Nullable @Named("showBodyDefaultFalse") Boolean showBody,
                                        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (client == null)
            client = "isha";

        if (showBody == null)
            showBody = false;

        try {

            login = Utils.getLoginEmail(user);

            Long startMS = null;
            if (dateRange != null)
                startMS = new Date().getTime() - DateUtils.getMilliSecondsFromDateRange(dateRange);

            List<MailContentEntity> entities = MailContent.query(client, owner, startMS, null, login);

            List<MailContentProp> props = new ArrayList<>();
            for (MailContentEntity entity : entities) {
                MailContentProp mailContentProp = entity.toProp();
                mailContentProp.bodyUrl =
                        req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                                + "/mailContent/get?client=" + client + "&mailContentId="
                                + mailContentProp.mailContentId;

                if (!showBody)
                    mailContentProp.body = null;

                props.add(mailContentProp);
            }

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "sendEmail", httpMethod = HttpMethod.GET)
    public APIResponse sendEmail(@Named("client") String client,
                                 @Named("firstNames") List<String> firstNames, @Named("lastNames") List<String> lastNames,
                                 @Named("emails") List<String> emails, @Named("mailContentId") long mailContentId,
                                 @Nullable @Named("groupOtherIdOrName") String groupIdOrName,
                                 @Named("groupDropdown") GroupEnum groupEnum, @Nullable @Named("tags") Set<String> tags,
                                 @Named("senderDropdown") IshaEmailSender senderEnum,
                                 @Nullable @Named("senderOther") String sender,
                                 @Nullable @Named("suppressIfAlreadySentDefaultTrue") Boolean suppressIfAlreadySent,
                                 @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (null == client)
            client = "isha";

        try {

            StopWatch stopWatch = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);

            if (groupEnum == GroupEnum.OTHER)
                ensure(groupIdOrName != null,
                        "groupOtherIdOrName should be specified when groupDropDown is 'OTHER'");

            if (groupEnum != GroupEnum.OTHER)
                groupIdOrName = groupEnum.toString();

            long groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;

            if (senderEnum == IshaEmailSender.OTHER)
                ensure(sender != null, "senderOther should be specified with senderEnum is 'OTHER'");

            if (senderEnum != IshaEmailSender.OTHER)
                sender =
                        senderEnum.toString().toLowerCase().replace("at", "@").replace("dot", ".")
                                .replace("_", "");

            Utils.ensureValidEmail(sender);

            if (emails.isEmpty())
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "No email address specified");

            ensureEqual(emails.size(), firstNames.size(), "Size mismatch. [" + emails.size()
                    + "] emails specified but [" + firstNames.size() + "] first names specified");

            ensureEqual(emails.size(), lastNames.size(), "Size mismatch. [" + emails.size()
                    + "] emails specified but [" + lastNames.size() + "] last names specified");

            MailMap mailMap = new MailMap();
            for (int i = 0; i < emails.size(); i++) {
                mailMap.add(emails.get(i), firstNames.get(i), lastNames.get(i));
            }

            if (suppressIfAlreadySent == null) {
                suppressIfAlreadySent = true;
            }

            MailSendInput msi = new MailSendInput();
            msi.createMember = true;
            msi.groupId = groupId;
            msi.mailContentId = mailContentId;
            msi.isTransactionEmail = false;
            msi.senderEmail = sender;
            msi.tags = tags;
            msi.suppressIfAlreadySent = suppressIfAlreadySent;

            List<SentMailEntity> sentMailEntities = Mail.send(client, msi, mailMap, login);

            List<SentMailProp> sentMailProps = new ArrayList<>();
            for (SentMailEntity sentMailEntity : sentMailEntities) {
                sentMailProps.add(sentMailEntity.toProp());
            }

            return new APIResponse().status(Status.SUCCESS)
                    .message("Sent [" + sentMailEntities.size() + "] emails").object(sentMailProps)
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "sendEmailToList", httpMethod = HttpMethod.GET)
    public APIResponse sendEmailToList(@Named("client") ClientEnum clientEnum,
                                       @Nullable @Named("clientIfOther") String clientOther, @Named("listId") long listId,
                                       @Named("mailContentId") long mailContentId, @Nullable @Named("tags") Set<String> tags,
                                       @Named("senderDropdown") IshaEmailSender senderEnum,
                                       @Nullable @Named("senderOther") String sender,
                                       @Nullable @Named("firstNameIfMissingDefaultIsha") String defaultFirstName,
                                       @Nullable @Named("lastNameIfMissingDefaultIsha") String defaultLastName,
                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        if (defaultFirstName == null)
            defaultFirstName = "isha";

        if (defaultLastName == null)
            defaultLastName = "isha";

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);

            StopWatch stopWatch = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);

            if (senderEnum == IshaEmailSender.OTHER)
                ensure(sender != null, "senderOther should be specified with senderEnum is 'OTHER'");

            if (senderEnum != IshaEmailSender.OTHER)
                sender =
                        senderEnum.toString().toLowerCase().replace("at", "@").replace("dot", ".")
                                .replace("_", "");

            Utils.ensureValidEmail(sender);

            List<SentMailEntity> sentMailEntities =
                    Mail.sendToList(client, listId, mailContentId, sender, tags, login, defaultFirstName,
                            defaultLastName);

            List<SentMailProp> sentMailProps = new ArrayList<>();
            for (SentMailEntity sentMailEntity : sentMailEntities) {
                sentMailProps.add(sentMailEntity.toProp());
            }

            return new APIResponse().status(Status.SUCCESS)
                    .message("Sent [" + sentMailEntities.size() + "] emails").object(sentMailProps)
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "sendEmailToListDropdown", httpMethod = HttpMethod.GET)
    public APIResponse sendEmailToListDropdown(@Named("client") ClientEnum clientEnum,
                                               @Nullable @Named("clientIfOther") String clientOther, @Named("group") GroupEnum groupEnum,
                                               @Named("list") ListApi.ListEnum listEnum, @Named("mailContentId") long mailContentId,
                                               @Nullable @Named("tags") Set<String> tags,
                                               @Named("senderDropdown") IshaEmailSender senderEnum,
                                               @Nullable @Named("senderOther") String sender,
                                               @Nullable @Named("firstNameIfMissingDefaultIsha") String defaultFirstName,
                                               @Nullable @Named("lastNameIfMissingDefaultIsha") String defaultLastName,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        if (defaultFirstName == null)
            defaultFirstName = "isha";

        if (defaultLastName == null)
            defaultLastName = "isha";

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            long groupId = Group.safeGetByIdOrName(client, groupEnum.toString()).toProp().groupId;

            long listId =
                    crmdna.list.List.safeGetByGroupIdAndName(client, groupId, listEnum.toString()).toProp().listId;

            return sendEmailToList(clientEnum, clientOther, listId, mailContentId, tags, senderEnum, sender,
                    defaultFirstName, defaultLastName, showStackTrace, req, user);


        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "sendEmailToParticipantsIfPresentInList", httpMethod = HttpMethod.GET)
    public APIResponse sendEmailToParticipantsIfPresentInList(@Named("client") ClientEnum clientEnum,
                                               @Nullable @Named("clientIfOther") String clientOther, @Named("group") GroupEnum groupEnum,
                                               @Named("list") ListApi.ListEnum listEnum, @Named("mailContentId") long mailContentId,
                                               @Named("programId") long programId,
                                               @Named("senderDropdown") IshaEmailSender senderEnum,
                                               @Nullable @Named("senderOther") String sender,
                                               @Nullable @Named("firstNameIfMissingDefaultIsha") String defaultFirstName,
                                               @Nullable @Named("lastNameIfMissingDefaultIsha") String defaultLastName,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        String client = null;

        if (defaultFirstName == null)
            defaultFirstName = "isha";

        if (defaultLastName == null)
            defaultLastName = "isha";

        try {
            StopWatch stopWatch = StopWatch.createStarted();
            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            long groupId = Group.safeGetByIdOrName(client, groupEnum.toString()).toProp().groupId;

            long listId =
                    crmdna.list.List.safeGetByGroupIdAndName(client, groupId, listEnum.toString()).toProp().listId;

            if (senderEnum == IshaEmailSender.OTHER)
                ensure(sender != null, "senderOther should be specified with senderEnum is 'OTHER'");

            if (senderEnum != IshaEmailSender.OTHER)
                sender =
                        senderEnum.toString().toLowerCase().replace("at", "@").replace("dot", ".")
                                .replace("_", "");

            List<SentMailEntity> sentMailEntities = Mail.sendToParticipantsIfPresentInList(client, programId, listId, mailContentId,
                    sender, defaultFirstName, defaultLastName, login);

            List<SentMailProp> sentMailProps = new ArrayList<>();
            for (SentMailEntity sentMailEntity : sentMailEntities) {
                sentMailProps.add(sentMailEntity.toProp());
            }

            return new APIResponse().status(Status.SUCCESS)
                    .message("Sent [" + sentMailEntities.size() + "] emails").object(sentMailProps)
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "sendEmailToAddressesInPublishedGoogleSheet", httpMethod = HttpMethod.GET)
    public APIResponse sendEmailToAddressesInPublishedGoogleSheet(
            @Named("client") String client, @Named("spreadSheetKey") String gsKey,
            @Nullable @Named("numLinesToReadExclHeaderDefault4000") Integer numLinesExclHeader,
            @Named("mailContentId") long mailContentId, @Nullable @Named("tags") Set<String> tags,
            @Nullable @Named("groupOtherIdOrName") String groupIdOrName,
            @Named("groupDropdown") GroupEnum groupEnum,
            @Named("senderDropdown") IshaEmailSender senderEnum,
            @Nullable @Named("senderOther") String sender,
            @Nullable @Named("firstNameIfMissingDefaultIsha") String defaultFirstName,
            @Nullable @Named("lastNameIfMissingDefaultIsha") String defaultLastName,
            @Nullable @Named("suppressIfAlreadySentDefaultTrue") Boolean suppressIfAlreadySent,
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (null == client)
            client = "isha";

        try {
            StopWatch stopWatch = StopWatch.createStarted();

            if (defaultFirstName == null)
                defaultFirstName = "Isha";

            if (defaultLastName == null)
                defaultLastName = "Isha";

            login = Utils.getLoginEmail(user);

            if (groupEnum == GroupEnum.OTHER)
                ensure(groupIdOrName != null,
                        "groupOtherIdOrName should be specified when groupDropDown is 'OTHER'");
            if (groupEnum != GroupEnum.OTHER)
                groupIdOrName = groupEnum.toString();

            long groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;

            if (senderEnum == IshaEmailSender.OTHER)
                ensure(sender != null, "senderOther should be specified with senderEnum is 'OTHER'");

            if (senderEnum != IshaEmailSender.OTHER)
                sender =
                        senderEnum.toString().toLowerCase().replace("at", "@").replace("dot", ".")
                                .replace("_", "");

            Utils.ensureValidEmail(sender);

            if (numLinesExclHeader == null)
                numLinesExclHeader = 4000;

            List<Map<String, String>> listOfMap =
                    GSpreadSheet.getPublishedSpreasheetAsListOfMap(gsKey, numLinesExclHeader);

            ensure(!listOfMap.isEmpty(), "No data found is spreadsheet");

            ValidationResultProp validationResultProp = validate(listOfMap);
            if (validationResultProp.hasErrors())
                return new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT).object(
                        validationResultProp);

            ensureNotNull(defaultFirstName, "defaultFirstName is null");
            ensureNotNull(!defaultFirstName.isEmpty(), "defaultFirstName is empty");
            ensureNotNull(defaultLastName, "defaultLastName is null");
            ensureNotNull(!defaultLastName.isEmpty(), "defaultLastName is empty");

            MailMap mailMap = MailMapFactory.getFromListOfMap(listOfMap, defaultFirstName,
                defaultLastName);

            if (suppressIfAlreadySent == null) {
                suppressIfAlreadySent = true;
            }

            MailSendInput msi = new MailSendInput();
            msi.createMember = true;
            msi.groupId = groupId;
            msi.isTransactionEmail = false;
            msi.mailContentId = mailContentId;
            msi.senderEmail = sender;
            msi.tags = tags;
            msi.suppressIfAlreadySent = suppressIfAlreadySent;

            List<SentMailEntity> sentMailEntities = Mail.send(client, msi, mailMap, login);

            List<SentMailProp> sentMailProps = new ArrayList<>();
            for (SentMailEntity sentMailEntity : sentMailEntities) {
                sentMailProps.add(sentMailEntity.toProp());
            }

            return new APIResponse().status(Status.SUCCESS)
                    .message("Sent [" + sentMailEntities.size() + "] emails").object(sentMailProps)
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "validateEmailAddressSpreadsheet", httpMethod = HttpMethod.GET)
    public APIResponse validateEmailAddressSpreadsheet(@Named("client") String client,
                                                       @Named("spreadSheetKey") String gsKey,
                                                       @Nullable @Named("numLinesExclHeaderDefault4000") Integer numLinesExclHeader,
                                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String login = null;

        if (null == client)
            client = "isha";

        try {

            if (numLinesExclHeader == null)
                numLinesExclHeader = 4000;

            List<Map<String, String>> listOfMap =
                    GSpreadSheet.getPublishedSpreasheetAsListOfMap(gsKey, numLinesExclHeader);

            ensure(!listOfMap.isEmpty(), "No data found is spreadsheet");

            ValidationResultProp validationResultProp = validate(listOfMap);
            if (validationResultProp.hasErrors())
                return new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT).object(
                        validationResultProp);

            return new APIResponse().status(Status.SUCCESS).object(validationResultProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "querySentMail", httpMethod = HttpMethod.GET)
    public APIResponse querySentMail(@Named("client") String client,
                                     @Nullable @Named("memberId") Long memberId, @Nullable @Named("email") String email,
                                     @Nullable @Named("mailContentId") Long mailContentId,
                                     @Nullable @Named("tags") Set<String> tags, @Nullable @Named("open") Boolean open,
                                     @Nullable @Named("click") Boolean click, @Nullable @Named("mobileClick") Boolean mobileClick,
                                     @Nullable @Named("reject") Boolean reject, @Nullable @Named("softBounce") Boolean softBounce,
                                     @Nullable @Named("hardBounce") Boolean hardBounce, @Nullable @Named("defer") Boolean defer,
                                     @Nullable @Named("clickUrls") Set<String> clickUrls,
                                     @Nullable @Named("dateRange") DateRange dateRange,
                                     @Nullable @Named("numResultsDefault10") Integer numResults,
                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (client == null)
            client = "isha";

        try {

            login = Utils.getLoginEmail(user);

            SentMailQueryCondition qc = new SentMailQueryCondition();

            qc.memberId = memberId;
            qc.email = email;
            qc.mailContentId = mailContentId;
            qc.tags = tags;
            qc.open = open;
            qc.click = click;
            qc.mobileClick = mobileClick;
            qc.reject = reject;
            qc.softBounce = softBounce;
            qc.hardBounce = hardBounce;
            qc.defer = defer;
            qc.clickUrls = clickUrls;

            if (dateRange != null) {
                qc.endMS = new Date().getTime();
                qc.startMS = qc.endMS - DateUtils.getMilliSecondsFromDateRange(dateRange);
            }

            if (numResults == null)
                numResults = 10;

            qc.numResults = numResults;

            List<SentMailEntity> entities = Mail.queryEntitiesSortedByTimeDesc(client, qc, login);

            List<SentMailProp> props = new ArrayList<>(entities.size());
            for (SentMailEntity entity : entities) {
                props.add(entity.toProp());
            }

            SentMailProp.populateDependents(client, props);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "getStatsByMailContent", httpMethod = HttpMethod.GET)
    public APIResponse getStatsByMailContent(@Named("client") String client,
                                             @Named("mailContentId") Long mailContentId,
                                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (client == null)
            client = "isha";

        try {
            StopWatch stopWatch = StopWatch.createStarted();
            login = Utils.getLoginEmail(user);

            MailStatsProp mailStatsProp = Mail.getStatsByMailContent(client, mailContentId, login);

            return new APIResponse().status(Status.SUCCESS).object(mailStatsProp)
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    @ApiMethod(path = "getHrefsInMailContent", httpMethod = HttpMethod.GET)
    public APIResponse getHrefsInMailContent(@Named("client") String client,
                                             @Nullable @Named("mailContentId") Long mailContentId,
                                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        if (client == null)
            client = "isha";

        try {
            StopWatch stopWatch = StopWatch.createStarted();
            login = Utils.getLoginEmail(user);

            MailContentProp mailContentProp = MailContent.safeGet(client, mailContentId).toProp();

            Set<String> hrefs = Utils.getHrefs(mailContentProp.body);

            return new APIResponse().status(Status.SUCCESS).object(hrefs)
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login)
                    .client(client));
        }
    }

    public enum IshaEmailSender {
        SINGAPORE_AT_ISHAYOGA_DOT_ORG, SINGAPORE_AT_INNERENGINEERING_DOT_COM, INFO_AT_BHAIRAVIYOGA_DOT_SG, OTHER
    }
}
