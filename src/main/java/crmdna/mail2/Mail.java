package crmdna.mail2;

import com.google.appengine.api.utils.SystemProperty;
import com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import crmdna.client.Client;
import crmdna.client.ClientEntity;
import crmdna.common.DateUtils;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.list.ListProp;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberQueryCondition;
import crmdna.program.Program;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;

public class Mail {

    public static final int MAX_EMAILS_PER_SEND = 5000;
    static final int MAX_URL_LENGTH = 100;
    static final String SYSTEM_PROPERTY_SUPPRESS_EMAIL = "SUPPRESS_EMAIL";
    private static IBounceHandler bounceHandler;

    public static synchronized void setBounceHandler(IBounceHandler bounceHandler) {
        ensureNotNull(bounceHandler, "bounceHandler is null");
        Mail.bounceHandler = bounceHandler;
    }

    public static MailStatsProp getStatsByTag(String client, Set<String> tags) {
        Client.ensureValid(client);

        ensureNotNull(tags, "tags is null");
        ensureNoNullElement(tags);

        if (tags.isEmpty())
            return new MailStatsProp();

        List<TagSetEntity> tagSetEntities = TagSet.query(client, tags);

        if (tagSetEntities.isEmpty())
            return new MailStatsProp();

        List<Long> tagSetIds = new ArrayList<>(tagSetEntities.size());
        for (TagSetEntity tagSetEntity : tagSetEntities) {
            tagSetIds.add(tagSetEntity.tagSetId);
        }

        ensure(!tagSetIds.isEmpty(), "tagSetIds is empty");

        Query<SentMailEntity> q =
                ofy(client).load().type(SentMailEntity.class).filter("tagSetId in", tagSetIds);

        return getMailStatsProp(client, q, null);
    }

    public static MailStatsProp getStatsByMailContent(String client, long mailContentId, String login) {
        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        MailContentProp mailContentProp = MailContent.safeGet(client, mailContentId).toProp();
        Set<String> urls = Utils.getHrefs(mailContentProp.body);

        Query<SentMailEntity> q =
                ofy(client).load().type(SentMailEntity.class).filter("mailContentId", mailContentId);

        return getMailStatsProp(client, q, urls);
    }

    private static MailStatsProp getMailStatsProp(String client, Query<SentMailEntity> q,
                                                  Set<String> urls) {

        MailStatsProp mailStatsProp = new MailStatsProp();
        mailStatsProp.numRecipientsSendAttempted = q.count();

        mailStatsProp.rejects = q.filter("reject", true).count();
        mailStatsProp.defers = q.filter("defer", true).count();
        mailStatsProp.hardBounces = q.filter("hardBounce", true).count();
        mailStatsProp.softBounces = q.filter("softBounce", true).count();
        mailStatsProp.numRecipientsSent =
                mailStatsProp.numRecipientsSendAttempted - mailStatsProp.rejects
                        - mailStatsProp.hardBounces - mailStatsProp.softBounces;

        mailStatsProp.numRecipientsThatOpened = q.filter("open", true).count();
        mailStatsProp.numRecipientsThatClickedALink = q.filter("click", true).count();
        mailStatsProp.numRecipientsThatClickedALinkFromMobile = q.filter("mobile", true).count();
        mailStatsProp.numRecipientsThatReportedAsSpam = q.filter("spam", true).count();

        List<Key<SentMailEntity>> clickKeys = q.filter("click", true).keys().list();
        List<SentMailEntity> countryCities = new ArrayList<>();
        if (!clickKeys.isEmpty())
            countryCities =
                    ofy(client).load().type(SentMailEntity.class).filterKey("in", clickKeys)
                            .project("countryCity").list();

        final String NOT_AVAILABLE = "N.A";
        for (SentMailEntity entity : countryCities) {
            if ((entity.countryCity == null) || !entity.countryCity.contains("/"))
                entity.countryCity = NOT_AVAILABLE + "/" + NOT_AVAILABLE;

            String split[] = entity.countryCity.split(Pattern.quote("/"));

            String country = NOT_AVAILABLE;
            if (split.length == 2) {
                country = split[0];
            }

            Map<String, Integer> map = mailStatsProp.countryVsNumRecipientsThatClickedALink;
            if (!map.containsKey(country))
                map.put(country, 0);
            map.put(country, map.get(country) + 1);

            map = mailStatsProp.cityVsNumRecipientsThatClickedALink;
            if (!map.containsKey(entity.countryCity))
                map.put(entity.countryCity, 0);

            map.put(entity.countryCity, map.get(entity.countryCity) + 1);
        }

        Map<String, Integer> map = mailStatsProp.urlVsNumRecipientsThatClicked;
        if (urls != null) {
            for (String url : urls) {
                if ((url != null) && !url.isEmpty()) {
                    url = Utils.getFirstNChar(url, MAX_URL_LENGTH);

                    int count = q.filter("urls", url).count();
                    map.put(url, count);
                }
            }
        }

        return mailStatsProp;
    }

    static SentMailEntity getIfExistsElseNull(String client, long mailId) {
        Client.ensureValid(client);

        return ofy(client).load().type(SentMailEntity.class).id(mailId).now();
    }

    static SentMailEntity safeGet(String client, long mailId) {
        Client.ensureValid(client);

        SentMailEntity entity = ofy(client).load().type(SentMailEntity.class).id(mailId).now();

        if (entity == null)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Cannot find mail id [" + mailId + "] for client [" + client + "]");

        return entity;
    }

    public static List<SentMailEntity> sendBespoke(String client, long groupId, MailMap mailMap,
                                                   String subject, String messageBody, String from,
                                                   Set<String> tags, String login)
            throws MandrillApiError, IOException {

        Client.ensureValid(client);
        String displayName = "Bespoke_" + DateUtils.getNanoSeconds(new Date());
        long mailContentId = MailContent
            .create(client, displayName, groupId, subject, messageBody, login).mailContentId;

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = groupId;
        msi.mailContentId = mailContentId;
        msi.isTransactionEmail = false;
        msi.senderEmail = from;
        msi.suppressIfAlreadySent = false;
        msi.tags = tags;
        return send(client, msi, mailMap, login);
    }

    public static List<SentMailEntity> sendToList(String client, long listId, long mailContentId,
                                                  String sender, Set<String> tags, String login, String defaultFirstName, String defaultLastName)
            throws MandrillApiError, IOException {

        Client.ensureValid(client);
        ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

        if (!listProp.enabled)
            throw new APIException("Cannot send emails to disabled list [" + listId + "]")
                    .status(Status.ERROR_PRECONDITION_FAILED);

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(listId);

        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, listProp.groupId, defaultFirstName,
                defaultLastName, login);

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = listProp.groupId;
        msi.mailContentId = mailContentId;
        msi.isTransactionEmail = false;
        msi.senderEmail = sender;
        msi.overrideSubject = null;
        msi.suppressIfAlreadySent = true;
        msi.tags = tags;

        return send(client, msi, mailMap, login);
    }

    public static List<SentMailEntity> sendToParticipantsIfPresentInList(String client, long programId, long listId, long mailContentId,
                                                  String sender, String defaultFirstName, String defaultLastName, String login)
            throws MandrillApiError, IOException {

        Client.ensureValid(client);

        Program.safeGet(client, programId);
        ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

        if (!listProp.enabled)
            throw new APIException("Cannot send emails to disabled list [" + listId + "]")
                    .status(Status.ERROR_PRECONDITION_FAILED);

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.programIds.add(programId);
        mqc.listIds.add(listId);

        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, listProp.groupId, defaultFirstName,
                defaultLastName, login);

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = listProp.groupId;
        msi.mailContentId = mailContentId;
        msi.isTransactionEmail = false;
        msi.senderEmail = sender;
        msi.overrideSubject = null;
        msi.suppressIfAlreadySent = true;

        return send(client, msi, mailMap, login);
    }

    public static List<SentMailEntity> send(String client, MailSendInput msi, MailMap mailMap,
                                            String login)
            throws MandrillApiError, IOException {

        Client.ensureValid(client);

        ensureNotNullNotEmpty(msi.senderEmail, "senderEmail is not specified");

        String subaccount = client;
        String fromName;
        if (msi.groupId != null) {
            Group.safeGet(client, msi.groupId);
            User.ensureGroupLevelPrivilege(client, msi.groupId, login, GroupLevelPrivilege.SEND_EMAIL);
            subaccount = client + "." + msi.groupId;
            fromName = Group.safeGetSenderNameFromEmail(client, msi.groupId, msi.senderEmail);

            if (! msi.isTransactionEmail) {
                removeUnsubscribedEmails(client, msi.groupId, mailMap, login);
            }
        } else {
            ensure(msi.isTransactionEmail,
                    "Only transactional email allowed at client level");
            fromName = Client.safeGetSenderNameFromEmail(client, msi.senderEmail);
        }

        //for now disallow bulk transactional emails
        if (msi.isTransactionEmail) {
            ensure(mailMap.size() == 1,
                    "Only 1 transaction email can be sent at a time. Attempt to send [" + mailMap.size() + "]");
        }

        if (msi.tags == null)
            msi.tags = new HashSet<>();
        ensureNoNullElement(msi.tags);

        if (msi.suppressIfAlreadySent) {
            Set<String> alreadySentEmails = getAlreadySentEmails(client, msi.mailContentId);

            for (String email : alreadySentEmails) {
                mailMap.delete(email);
            }
        }

        if (mailMap.getEmails().isEmpty())
            return new ArrayList<>();

        MailContentProp mailContentProp = MailContent.safeGet(client, msi.mailContentId).toProp();
        String subject = msi.overrideSubject == null ? mailContentProp.subject : msi.overrideSubject;
        String htmlBody = mailContentProp.body;

        ensureNotNull(subject, "Subject is null for mail content id [" + msi.mailContentId + "]");
        ensureNotNull(htmlBody, "Body is null for mail content id [" + msi.mailContentId + "]");

        EmailConfig emailConfig = Group.getEmailConfig(client, msi.groupId, User.SUPER_USER);

        Map<String, String> globalMetaData = new HashMap<>();
        globalMetaData.put(MetaData.CLIENT.toString(), client);

        mailMap.populateMailIds();

        String appId = SystemProperty.applicationId.get();
        if ((appId != null) && !appId.equalsIgnoreCase("ishacrmserver")) {
            emailConfig.mandrillApiKey = "E_71qbN55EqqUKEOLQghcQ"; //Test Key
        }

        if (!isEmailSuppressed())
            Mandrill.send(emailConfig.mandrillApiKey, mailMap, subject, htmlBody, msi.senderEmail, fromName,
                    subaccount, globalMetaData, msi.tags);

        Long tagSetId = null;
        if (!msi.tags.isEmpty())
            tagSetId = TagSet.getIfExistsElseCreateAndGet(client, msi.tags).tagSetId;

        List<SentMailEntity> sentMailEntities = new ArrayList<>(mailMap.size());
        for (String email : mailMap.getEmails()) {
            SentMailEntity sentMailEntity = new SentMailEntity();
            sentMailEntity.sentMailId = mailMap.getMailId(email);
            sentMailEntity.email = email;
            sentMailEntity.from = msi.senderEmail;
            sentMailEntity.tagSetId = tagSetId;
            sentMailEntity.mailContentId = msi.mailContentId;

            sentMailEntities.add(sentMailEntity);
        }

        if (msi.createMember) {
            Map<String, Long> emailVsMemberId =
                    Member.getMemberIdFromEmailIfExistsElseCreateAndGet(client, mailMap, msi.groupId);
            ensureEqual(mailMap.size(), emailVsMemberId.size(),
                    "Cannot get memberId for all emails");
            ensure(emailVsMemberId.keySet().containsAll(mailMap.getEmails()),
                    "Cannot get memberId for all emails");

            for (SentMailEntity sentMailEntity : sentMailEntities) {
                sentMailEntity.memberId = emailVsMemberId.get(sentMailEntity.email);
            }
        }

        ensureEqual(mailMap.size(), sentMailEntities.size());

        ofy(client).save().entities(sentMailEntities);

        return sentMailEntities;
    }

    static void removeUnsubscribedEmails(String client, long groupId, MailMap mailMap, String login) {

        Client.ensureValid(client);
        Group.safeGet(client, groupId);

        TreeSet<String> unsubscribedEmails = MemberLoader.getUnsubscribedEmails(client, groupId, login);

        for (String email : unsubscribedEmails) {
            mailMap.delete(email);
        }
    }

    static Set<String> getAlreadySentEmails(String client, long mailContentId) {
        Client.ensureValid(client);

        List<SentMailEntity> entities =
                ofy(client).load().type(SentMailEntity.class).filter("mailContentId", mailContentId)
                        .project("email").list();

        Set<String> emails = new HashSet<>();
        for (SentMailEntity sentMailEntity : entities) {
            emails.add(sentMailEntity.email);
        }

        return emails;
    }

    private static List<Key<SentMailEntity>> queryKeys(String client, SentMailQueryCondition qc,
                                                       String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        ensureNotNull(qc, "query condition is null");

        if (qc.tags == null)
            qc.tags = new HashSet<>();

        if (qc.clickUrls == null)
            qc.clickUrls = new HashSet<>();

        List<TagSetEntity> tagSetEntities = new ArrayList<>();
        if (!qc.tags.isEmpty())
            tagSetEntities = TagSet.query(client, qc.tags);

        if (!qc.clickUrls.isEmpty()) {
            Set<String> urlsMax100Char = new HashSet<>();

            for (String url : qc.clickUrls) {
                urlsMax100Char.add(Utils.getFirstNChar(url, 100));
            }

            qc.clickUrls = urlsMax100Char;
        }

        Set<Long> tagSetIds = new HashSet<>();
        if (!qc.tags.isEmpty()) {
            if (tagSetEntities.isEmpty())
                return new ArrayList<>();

            for (TagSetEntity tagSetEntity : tagSetEntities)
                tagSetIds.add(tagSetEntity.tagSetId);

            ensureEqual(tagSetEntities.size(), tagSetIds.size());
        }

        // build query condition
        Query<SentMailEntity> q = ofy(client).load().type(SentMailEntity.class);
        if (qc.memberId != null)
            q = q.filter("memberId", qc.memberId);

        if (qc.email != null)
            q = q.filter("email", qc.email);

        if (qc.mailContentId != null)
            q = q.filter("mailContentId", qc.mailContentId);

        if (!qc.tags.isEmpty())
            q = q.filter("tagSetId in", tagSetIds);

        if (qc.open != null) {
            ensure(qc.open, "Cannot query for open = false");
            q = q.filter("open", true);
        }

        if (qc.mobileClick != null) {
            ensure(qc.mobileClick, "Cannot query for mobileOpen = false");
            q = q.filter("mobile", true);
        }

        if (qc.click != null) {
            ensure(qc.click, "Cannot query for click = false");
            q = q.filter("click", true);
        }

        if (qc.reject != null) {
            ensure(qc.reject, "Cannot query for reject = false");
            q = q.filter("reject", true);
        }

        if (qc.softBounce != null) {
            ensure(qc.softBounce, "Cannot query for softBounce = false");
            q = q.filter("softBounce", true);
        }

        if (qc.hardBounce != null) {
            ensure(qc.hardBounce, "Cannot query for hardBounce = false");
            q = q.filter("hardBounce", true);
        }

        if (qc.defer != null) {
            ensure(qc.defer, "Cannot query for defer = false");
            q = q.filter("defer", true);
        }

        if (!qc.clickUrls.isEmpty())
            q = q.filter("urls in", qc.clickUrls);

        List<Key<SentMailEntity>> keys = q.keys().list();

        keys = removeExtra(keys, qc.startMS, qc.endMS, qc.numResults);

        return keys;
    }

    private static List<Key<SentMailEntity>> removeExtra(List<Key<SentMailEntity>> keys,
                                                         Long startMS, Long endMS, Integer numResults) {

        List<Long> ids = new ArrayList<>();
        for (Key<SentMailEntity> key : keys) {
            long id = key.getId();

            final int MILLION = 1000000;
            if ((startMS != null) && (id < startMS * MILLION))
                continue;
            if ((endMS != null) && (id > endMS * MILLION))
                continue;

            ids.add(key.getId());
        }

        Collections.sort(ids);

        if ((numResults == null) || (numResults > ids.size())) {
            numResults = ids.size();
        }

        Collections.reverse(ids);

        ids = ids.subList(0, numResults);

        keys = new ArrayList<>();
        for (long id : ids) {
            keys.add(Key.create(SentMailEntity.class, id));
        }

        return keys;
    }

    public static List<SentMailEntity> queryEntitiesSortedByTimeDesc(String client,
                                                                     SentMailQueryCondition qc, String login) {

        // List<Key<SentMailEntity>> keys = queryKeys(client, qc, login);
        List<Key<SentMailEntity>> keys = queryKeys(client, qc, login);

        Map<Key<SentMailEntity>, SentMailEntity> map = ofy(client).load().keys(keys);

        List<SentMailEntity> entities = new ArrayList<>(map.size());

        for (Key<SentMailEntity> key : keys) {
            if (map.containsKey(key)) {
                entities.add(map.get(key));
            }
        }

        return entities;
    }

    static boolean isEmailSuppressed() {
        String value = System.getProperty(SYSTEM_PROPERTY_SUPPRESS_EMAIL);

        if (value == null)
            return false;

        if (value.toUpperCase().equals("TRUE") || value.toUpperCase().equals("1"))
            return true;

        return false;
    }

    public static void processWebhookEvents(String postData) throws UnsupportedEncodingException {

        List<MandrillEventProp> eventProps = Mandrill.getMandrillEventProps(postData);

        Logger logger = Logger.getLogger(Mail.class.getName());
        logger.info("Num events: " + eventProps.size());
        Map<String, List<MandrillEventProp>> clientVsEventProps =
                filterOutInvalidAndGroupByClient(eventProps);

        logger.info("Num clients: " + clientVsEventProps.size());

        // process events for each client
        for (String client : clientVsEventProps.keySet()) {
            // processWebhookEvents(client, clientVsEventProps.get(client));
            processWebhookEvents(client, clientVsEventProps.get(client));
        }
    }

    static Map<String, List<MandrillEventProp>> filterOutInvalidAndGroupByClient(
            List<MandrillEventProp> eventProps) {

        ensureNotNull(eventProps, "eventProps is null");

        Logger logger = Logger.getLogger(Mail.class.getName());
        // get all clients
        Set<String> clients = new HashSet<>();
        for (int i = 0; i < eventProps.size(); i++) {
            MandrillEventProp eventProp = eventProps.get(i);

            String client = eventProp.getClient();
            if (client == null) {
                logger.warning("Client not set for mandrill event with id [" + eventProp._id + "]");
                continue;
            }

            clients.add(client);
        }

        Map<String, ClientEntity> clientVsEntity = Client.getEntities(clients);

        Map<String, List<MandrillEventProp>> clientVsEventProps = new HashMap<>();
        for (MandrillEventProp eventProp : eventProps) {

            if (eventProp.getMailId() == null) {
                logger.warning("MAIL_ID not available in metadata for mandrill event with id ["
                        + eventProp._id + "]. Ignoring event.");
                continue;
            }

            String client = eventProp.getClient();
            if (!clientVsEntity.containsKey(client)) {
                logger.warning("Invalid client [" + client + "] for mandrill event with id ["
                        + eventProp._id + "]. Ignoring event.");
                continue;
            }

            if (!clientVsEventProps.containsKey(client))
                clientVsEventProps.put(client, new ArrayList<MandrillEventProp>());
            List<MandrillEventProp> list = clientVsEventProps.get(client);
            list.add(eventProp);
        }

        return clientVsEventProps;
    }

    private static void processWebhookEvents(String client, List<MandrillEventProp> eventProps) {
        Client.ensureValid(client);

        Set<Long> mailIds = new HashSet<>();

        for (MandrillEventProp eventProp : eventProps) {
            if (eventProp.getMailId() == null)
                continue;

            mailIds.add(eventProp.getMailId());
        }

        Logger logger = Logger.getLogger(Mail.class.getName());
        logger.info("Processing [" + eventProps.size() + "] mandrill events for client [" + client
                + "]");

        Map<Long, SentMailEntity> sentMailIdVsEntity =
                ofy(client).load().type(SentMailEntity.class).ids(mailIds);

        for (MandrillEventProp eventProp : eventProps) {
            if (!sentMailIdVsEntity.containsKey(eventProp.getMailId())) {
                logger.warning("Invalid mail id [" + eventProp.getMailId()
                        + "] for mandrill event with id [" + eventProp._id + "]. Ignoring event");
                continue;
            }

            SentMailEntity sentMailEntity = sentMailIdVsEntity.get(eventProp.getMailId());
            updateSentMailEntity(sentMailEntity, eventProp);
        }

        ofy(client).save().entities(sentMailIdVsEntity.values());
    }

    private static void updateSentMailEntity(SentMailEntity sentMailEntity,
                                             MandrillEventProp eventProp) {

        ensureNotNull(sentMailEntity, "sentMailEntity is null");
        ensureNotNull(eventProp, "mandrillEventProp is null");

        // events are: send, deferral, hard_bounce, soft_bounce, open, click, spam, unsub, reject
        if (eventProp.event == null)
            return;

        Logger logger = Logger.getLogger(Mail.class.getName());
        logger.info("eventProp: " + new Gson().toJson(eventProp));

        long ms = eventProp.ts * 1000;

        String event = eventProp.event;
        if (event.equals("send")) {
            sentMailEntity.sendMS = ms;
            sentMailEntity.defer = false;
            return;
        }

        if (event.equals("deferral")) {
            sentMailEntity.defer = true;
            return;
        }

        if (event.equals("hard_bounce")) {
            sentMailEntity.hardBounce = true;
            if (bounceHandler != null)
                bounceHandler.onHardBounce(eventProp);
            return;
        }

        if (event.equals("soft_bounce")) {
            sentMailEntity.softBounce = true;
            if (bounceHandler != null)
                bounceHandler.onSoftBounce(eventProp);

            return;
        }

        if (event.equals("open")) {
            sentMailEntity.open = true;
            sentMailEntity.openMS = ms;

            return;
        }

        if (event.equals("click")) {
            sentMailEntity.click = true;

            String country = "N.A";
            if (eventProp.location.country_short != null)
                country = eventProp.location.country_short;
            String city = "N.A";
            if (eventProp.location.city != null)
                city = eventProp.location.city;

            sentMailEntity.countryCity = country + "/" + city;
            sentMailEntity.mobile = eventProp.user_agent_parsed.mobile;

            if (eventProp.url != null)
                eventProp.url = Utils.getFirstNChar(eventProp.url, MAX_URL_LENGTH);

            sentMailEntity.clickMS.add(ms);
            sentMailEntity.urls.add(eventProp.url);
            return;
        }

        if (event.equals("spam")) {
            sentMailEntity.complaint = true;
            if (bounceHandler != null)
                bounceHandler.onComplaint(eventProp);
            return;
        }

        if (event.equals("unsub")) {
            // TODO
            return;
        }

        if (event.equals("reject")) {
            sentMailEntity.reject = true;
            return;
        }

        // ignore sync events for now
    }

    public enum MetaData {
        MAIL_ID, CLIENT
    }
}
