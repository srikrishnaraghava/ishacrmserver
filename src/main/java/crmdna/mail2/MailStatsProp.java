package crmdna.mail2;

import java.util.Map;
import java.util.TreeMap;

public class MailStatsProp {
    public int numRecipientsSendAttempted;
    public int numRecipientsSent;
    public int rejects;
    public int defers;
    public int softBounces;
    public int hardBounces;
    public int numRecipientsThatReportedAsSpam;

    public int numRecipientsThatOpened;

    public int numRecipientsThatClickedALink;
    public int numRecipientsThatClickedALinkFromMobile;

    public Map<String, Integer> countryVsNumRecipientsThatClickedALink = new TreeMap<>();
    public Map<String, Integer> cityVsNumRecipientsThatClickedALink = new TreeMap<>();

    public Map<String, Integer> urlVsNumRecipientsThatClicked = new TreeMap<>();
}
