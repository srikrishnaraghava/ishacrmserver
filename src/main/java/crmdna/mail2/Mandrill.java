package crmdna.mail2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microtripit.mandrillapp.lutung.MandrillApi;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.MergeVar;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.MergeVarBucket;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.Recipient;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.RecipientMetadata;
import com.microtripit.mandrillapp.lutung.view.MandrillMessageStatus;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.mail2.Mail.MetaData;
import crmdna.mail2.MailMap.MergeVarID;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;

import static crmdna.common.AssertUtils.*;

public class Mandrill {

    static void send(String apiKey, MailMap mailMap,
                     String subject, String messageBody, String from, String fromName, String subaccount,
                     Map<String, String> globalMetaData, Set<String> tags) throws MandrillApiError, IOException {

        ensureNotNull(messageBody, "messageBody is null");
        ensureNotNull(from, "from is null");
        ensureNotNull(mailMap, "mailMap is null");
        ensureNotNull(subject, "subject is null");
        ensureValidApiKey(apiKey);
        mailMap.validateMergeVars();

        Logger logger = Logger.getLogger(Mandrill.class.getName());
        logger.info("mailMap.size() = " + mailMap.size());

        if (mailMap.isEmpty())
            return;

        if (mailMap.size() > Mail.MAX_EMAILS_PER_SEND) {
            throw new APIException().status(Status.ERROR_OVERFLOW).message(
                    "Attempt to send [" + mailMap.size() + "] emails in one shot. Maximum allowed is ["
                            + Mail.MAX_EMAILS_PER_SEND + "]");
        }

        MandrillMessage message = new MandrillMessage();

        message.setSubject(subject);
        message.setHtml(messageBody);
        message.setAutoText(true);
        message.setTrackClicks(true);
        message.setTrackOpens(true);
        message.setFromEmail(from);
        message.setFromName(fromName);
        message.setInlineCss(true);
        message.setPreserveRecipients(false);
        message.setMergeLanguage("handlebars");

        if ((tags != null) && !tags.isEmpty())
            message.setTags(new ArrayList<>(tags));

        List<MergeVarBucket> mergeVarBuckets = new ArrayList<>(mailMap.size());
        List<MergeVar> globalMergeVars = new ArrayList<>();
        List<RecipientMetadata> recipientMetadataList = new ArrayList<>(mailMap.size());

        populateMergeVars(mailMap, globalMergeVars, mergeVarBuckets);

        for (String email : mailMap.getEmails()) {
            RecipientMetadata recipientMetadata = new RecipientMetadata();

            recipientMetadata.setRcpt(email);

            HashMap<String, String> map = new HashMap<>();
            map.put(MetaData.MAIL_ID.toString(), mailMap.get(MergeVarID.MAIL_ID, email));
            recipientMetadata.setValues(map);
            recipientMetadataList.add(recipientMetadata);
        }

        message.setMergeVars(mergeVarBuckets);
        message.setGlobalMergeVars(globalMergeVars);
        message.setRecipientMetadata(recipientMetadataList);
        message.setMetadata(globalMetaData);

        MandrillApi mandrillApi = new MandrillApi(apiKey);

        if ((subaccount != null) && !subaccount.isEmpty()) {
            if (!isSubaccountValid(apiKey, subaccount)) {
                createSubaccount(apiKey, subaccount);
            }

            ensureValidSubaccount(apiKey, subaccount); // just in case
            message.setSubaccount(subaccount);
        }

        List<Recipient> recipients = new ArrayList<>();

        Set<String> emails = mailMap.getEmails();
        for (String email : emails) {
            Recipient recipient = new Recipient();
            recipient.setEmail(email);
            recipients.add(recipient);
        }

        message.setTo(recipients);

        if (emails.size() == 1) {
            message.setBcc(from);
        }

        MandrillMessageStatus[] status = mandrillApi.messages().send(message, true);

        ensureEqual(emails.size(), status.length,
                "Num elements in MandrillMessageStatus does not match number of emails");
    }

    static List<MandrillEventProp> getMandrillEventProps(String postData)
            throws UnsupportedEncodingException {

        ensureNotNull(postData, "postData is null");

        String json = URLDecoder.decode(postData, "UTF-8");

        // json is of the format: mandrill_events=[{"event":....}]. remove mandrill_events=
        ensure(json.contains("mandrill_events="),
                "postData should start with 'mandrill_events=' (without quotes)");
        json = json.substring(16);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        MandrillEventProp[] mandrillEventProps = gson.fromJson(json, MandrillEventProp[].class);

        return Arrays.asList(mandrillEventProps);
        }

    public static void populateMergeVars(MailMap mailMap, List<MergeVar> globalMergeVars,
        List<MergeVarBucket> mergeVarBuckets) {

        for (String email : mailMap.getEmails()) {
            MergeVarBucket mergeVarBucket = new MergeVarBucket();
            mergeVarBucket.setRcpt(email);
            List<MergeVar> mergeVars = new ArrayList<>();

            for (MergeVarID mergeVarID : MergeVarID.values()) {
                String mergeVarValue = mailMap.get(mergeVarID, email);
                if (mergeVarValue != null) {
                    mergeVars.add(new MergeVar(mergeVarID.toString(), mergeVarValue));
                }
            }
            mergeVarBucket.setVars(mergeVars);
            mergeVarBuckets.add(mergeVarBucket);
        }

        for (MergeVarID mergeVarID : MergeVarID.values()) {
            String mergeVarValue = mailMap.get(mergeVarID);
            if (mergeVarValue != null) {
                globalMergeVars.add(new MergeVar(mergeVarID.toString(), mergeVarValue));
            } else {
                Object mergeVarObjectValue = mailMap.getObject(mergeVarID);
                if (mergeVarObjectValue != null) {
                    globalMergeVars.add(new MergeVar(mergeVarID.toString(), mergeVarObjectValue));
        }
            }
        }
    }

    public static String createSubaccount(String apiKey, String subaccount) throws IOException,
            MandrillApiError {
        ensureValidApiKey(apiKey);

        ensureNotNull(subaccount, "subaccount is null");
        // TODO: remove all non alpha numeric (here as well as in crmdna)
        subaccount = Utils.removeSpaceUnderscoreBracketAndHyphen(subaccount).toLowerCase();

        ensure(subaccount.length() > 0, "subaccount is empty string");

        subaccount = Utils.removeSpaceUnderscoreBracketAndHyphen(subaccount).toLowerCase();

        ensureValidApiKey(apiKey);

        if (isSubaccountValid(apiKey, subaccount)) {
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "subaccount [" + subaccount + "] already exists");
        }

        MandrillApi mandrillApi = new MandrillApi(apiKey);
        mandrillApi.subaccounts().add(subaccount, null, null, null);

        return subaccount;
    }

    public static void ensureValidApiKey(String apiKey) {
        ensureNotNull(apiKey, "apiKey is null");

        MandrillApi mandrillApi = new MandrillApi(apiKey);

        try {
            mandrillApi.users().ping();
        } catch (MandrillApiError | IOException e) {
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Mandrill Api Key [" + apiKey + "] is invalid");
        }
    }

    private static void ensureValidSubaccount(String alreadyValidatedApiKey, String subaccount)
            throws IOException {

        ensureNotNull(alreadyValidatedApiKey, "alreadyValidatedApiKey is null");
        ensureNotNull(subaccount, "subaccount is null");

        if (!isSubaccountValid(alreadyValidatedApiKey, subaccount)) {
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Subaccount [" + subaccount + "] not found for api key [" + alreadyValidatedApiKey + "]");
        }
    }

    private static boolean isSubaccountValid(String alreadyValidatedApiKey, String subaccount)
            throws IOException {
        ensureNotNull(alreadyValidatedApiKey, "alreadyValidatedApiKey is null");
        ensureNotNull(subaccount, "subaccount is null");

        MandrillApi mandrillApi = new MandrillApi(alreadyValidatedApiKey);

        try {
            mandrillApi.subaccounts().info(subaccount);
        } catch (MandrillApiError e) {
            return false;
        }

        return true;
    }
}
