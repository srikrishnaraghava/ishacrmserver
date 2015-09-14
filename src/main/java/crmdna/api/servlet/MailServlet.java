package crmdna.api.servlet;

import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.group.Group;
import crmdna.list.ListProp;
import crmdna.mail2.*;
import crmdna.registration.Registration;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MailServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            String login = ServletUtils.getLogin(request);

            try {
                if (action.equals("resendConfirmationEmail")) {

                    Registration.sendConfirmationEmail(client,
                            ServletUtils.getLongParam(request, "registrationId"));
                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("createMailContent")) {

                    String bodyUrl = ServletUtils.getStrParam(request, "bodyUrl");
                    String displayName = ServletUtils.getStrParam(request, "displayName");
                    String group = ServletUtils.getStrParam(request, "group");
                    String subject = ServletUtils.getStrParam(request, "subject");

                    long groupId = Group.safeGetByIdOrName(client, group).toProp().groupId;
                    String bodyHtml = Utils.readDataFromURL(bodyUrl);

                    MailContentProp mailContentProp =
                            MailContent.create(client, displayName, groupId, subject, bodyHtml, login);

                    mailContentProp.bodyUrl =
                            request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                                    + "/mailContent/get?client=" + client + "&mailContentId="
                                    + mailContentProp.mailContentId;

                    ServletUtils.setJson(response,
                            new APIResponse().status(Status.SUCCESS).object(mailContentProp));

                } else if (action.equals("queryMailContent")) {

                    Long daysAgo = ServletUtils.getLongParam(request, "daysAgo");
                    Long startMS = null;
                    if (daysAgo != null)
                        startMS = new Date().getTime() - (daysAgo * 86400 * 1000);

                    List<MailContentEntity> entities = MailContent.query(client, null, startMS, null, login);

                    List<MailContentProp> props = new ArrayList<>();
                    for (MailContentEntity entity : entities) {
                        MailContentProp mailContentProp = entity.toProp();
                        mailContentProp.bodyUrl =
                                request.getScheme() + "://" + request.getServerName() + ":"
                                        + request.getServerPort() + "/mailContent/get?client=" + client
                                        + "&mailContentId=" + mailContentProp.mailContentId;

                        mailContentProp.body = null;
                        props.add(mailContentProp);
                    }

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("updateMailContent")) {

                    long mailContentId = ServletUtils.getLongParam(request, "mailContentId");
                    String newBodyUrl = ServletUtils.getStrParam(request, "newBodyUrl");
                    String newDisplayName = ServletUtils.getStrParam(request, "newDisplayName");
                    String newSubject = ServletUtils.getStrParam(request, "newSubject");
                    Boolean allowUpdateIfMailsSent =
                            ServletUtils.getBoolParam(request, "allowUpdateIfMailsSent");

                    String newBodyHtml = Utils.readDataFromURL(newBodyUrl);

                    MailContentProp mailContentProp =
                            MailContent.update(client, mailContentId, newDisplayName, newSubject, newBodyHtml,
                                    allowUpdateIfMailsSent, login);

                    mailContentProp.bodyUrl =
                            request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                                    + "/mailContent/get?client=" + client + "&mailContentId="
                                    + mailContentProp.mailContentId;

                    ServletUtils.setJson(response,
                            new APIResponse().status(Status.SUCCESS).object(mailContentProp));

                } else if (action.equals("sendEmailToList")) {

                    long mailContentId = ServletUtils.getLongParam(request, "mailContentId");
                    long listId = ServletUtils.getLongParam(request, "listId");

                    String defaultFirstName = ServletUtils.getStrParam(request, "defaultFirstName");
                    if (defaultFirstName == null)
                        defaultFirstName = "Friend";

                    String defaultLastName = ServletUtils.getStrParam(request, "defaultLastName");
                    if (defaultLastName == null)
                        defaultLastName = "Friend";

                    String sender = ServletUtils.getStrParam(request, "sender");

                    List<SentMailEntity> sentMailEntities =
                            Mail.sendToList(client, listId, mailContentId, sender, null, login,
                                    defaultFirstName, defaultLastName);


                    List<SentMailProp> sentMailProps = new ArrayList<>();

                    for (SentMailEntity sentMailEntity : sentMailEntities) {
                        sentMailProps.add(sentMailEntity.toProp());
                    }

                    ServletUtils.setJson(response,
                            new APIResponse().status(Status.SUCCESS).object(sentMailProps));

                } else if (action.equals("sendToLoggedInUser")) {

                    MailMap mailMap = new MailMap();
                    mailMap.add(login, "FirstName", "LastName");

                    MailSendInput msi = new MailSendInput();
                    msi.createMember = false;
                    msi.groupId =  Group.safeGetByIdOrName(
                            client, ServletUtils.getStrParam(request, "group")).toProp().groupId;
                    msi.isTransactionEmail = false;
                    msi.mailContentId = ServletUtils.getLongParam(request, "mailContentId");
                    msi.senderEmail = ServletUtils.getStrParam(request, "sender");
                    msi.suppressIfAlreadySent = false;

                    List<SentMailEntity> sentMailEntities = Mail.send(client, msi, mailMap, login);

                    List<SentMailProp> sentMailProps = new ArrayList<>();
                    for (SentMailEntity sentMailEntity : sentMailEntities) {
                        sentMailProps.add(sentMailEntity.toProp());
                    }

                    ServletUtils.setJson(response,
                        new APIResponse().status(Status.SUCCESS).object(sentMailProps));

                } else if (action.equals("getLists")) {

                    long groupId = Group.safeGetByIdOrName(
                        client, ServletUtils.getStrParam(request, "group")).toProp().groupId;

                    List<ListProp> props = crmdna.list.List.querySortedProps(client, groupId);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else {
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }

            } catch (Exception ex) {
                ServletUtils.setJson(
                        response,
                        APIUtils.toAPIResponse(ex, true,
                                new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            String login = ServletUtils.getLogin(request);

            try {
                if (action.equals("getPlunkerContent")) {

                    String plunkerId  = ServletUtils.getStrParam(request, "plunkerId");

                    URL url = new URL("http://run.plnkr.co/plunks/" + plunkerId);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    reader.close();
                    response.setContentType("text/html");
                    response.getWriter().println(builder.toString());
                } else {
                    response.getWriter().println("ERROR");
                }

            } catch (Exception ex) {
                ServletUtils.setJson(
                        response,
                        APIUtils.toAPIResponse(ex, true,
                                new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }
}
