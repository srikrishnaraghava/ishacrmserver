package crmdna.mail2;

import com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

// TODO: consider removing this
class EmailStats {

    private String client;
    private List<Key<SentMailEntity>> recipients;
    private List<Key<SentMailEntity>> rejects;
    private List<Key<SentMailEntity>> defers;
    private List<Key<SentMailEntity>> hardBounces;
    private List<Key<SentMailEntity>> softBounces;
    private List<Key<SentMailEntity>> opens;
    private List<Key<SentMailEntity>> mobileClicks;
    private List<Key<SentMailEntity>> countryCityKeys;
    private List<Key<SentMailEntity>> clicks;
    private List<Key<SentMailEntity>> complaints;

    EmailStats(String client, Query<SentMailEntity> q) {
        Client.ensureValid(client);
        this.client = client;

        ensureNotNull(q, "query q is null");

        recipients = q.keys().list();
        System.out.println("recipients: " + new Gson().toJson(recipients));
        rejects = q.filter("reject", true).keys().list();
        defers = q.filter("defer", true).keys().list();
        hardBounces = q.filter("hardBounce", true).keys().list();
        softBounces = q.filter("softBounce", true).keys().list();
        opens = q.filter("open", true).keys().list();
        mobileClicks = q.filter("mobile", true).keys().list();
        // countryCities = q.filter("click", true).project("countryCity").list();
        countryCityKeys = q.filter("click", true).keys().list();
        clicks = q.filter("click", true).keys().list();
        complaints = q.filter("spam", true).keys().list();
    }

    MailStatsProp getStatsNow() {
        // this will be a blocking call

        ensureNotNull(recipients);
        ensureNotNull(rejects);
        ensureNotNull(defers);
        ensureNotNull(hardBounces);
        ensureNotNull(softBounces);
        ensureNotNull(opens);
        ensureNotNull(mobileClicks);
        ensureNotNull(countryCityKeys);
        ensureNotNull(clicks);
        ensureNotNull(complaints);

        List<SentMailEntity> countryCities = new ArrayList<>();
        if (!countryCityKeys.isEmpty())
            countryCities =
                    ofy(client).load().type(SentMailEntity.class).filterKey("in", countryCityKeys)
                            .project("countryCity").list();

        MailStatsProp prop = new MailStatsProp();

        prop.numRecipientsSendAttempted = recipients.size();
        prop.numRecipientsSendAttempted = prop.rejects = rejects.size();
        prop.defers = defers.size();
        prop.hardBounces = hardBounces.size();
        prop.softBounces = softBounces.size();

        prop.numRecipientsSent =
                prop.numRecipientsSendAttempted - prop.rejects - prop.defers - prop.hardBounces - prop.softBounces;

        prop.numRecipientsThatOpened = opens.size();
        prop.numRecipientsThatClickedALinkFromMobile = mobileClicks.size();

        final String NOT_AVAILABLE = "N.A";
        for (SentMailEntity entity : countryCities) {
            if ((entity.countryCity == null) || !entity.countryCity.contains("/"))
                entity.countryCity = NOT_AVAILABLE + "/" + NOT_AVAILABLE;

            String split[] = entity.countryCity.split(Pattern.quote("/"));

            String country = NOT_AVAILABLE;
            if (split.length == 2) {
                country = split[0];
            }

            Map<String, Integer> map = prop.countryVsNumRecipientsThatClickedALink;
            if (!map.containsKey(country))
                map.put(country, 0);
            map.put(country, map.get(country) + 1);

            map = prop.cityVsNumRecipientsThatClickedALink;
            if (!map.containsKey(entity.countryCity))
                map.put(entity.countryCity, 0);

            map.put(entity.countryCity, map.get(entity.countryCity) + 1);
        }

        prop.numRecipientsThatClickedALink = clicks.size();
        prop.numRecipientsThatReportedAsSpam = complaints.size();

        return prop;
    }
}
