package crmdna.member;

import java.util.TreeSet;

public class BulkSubscriptionResultProp {
    public TreeSet<String> existingMemberEmails = new TreeSet<>();
    public TreeSet<String> newMemberEmails = new TreeSet<>();

    public TreeSet<String> addedToListEmails = new TreeSet<>();

    public TreeSet<String> addedToGroupSubscriptionEmails = new TreeSet<>();
    public TreeSet<String> alreadySubscribedToGroupEmails = new TreeSet<>();

    public TreeSet<String> alreadyUnsubscribedToGroupEmails = new TreeSet<>();
}
