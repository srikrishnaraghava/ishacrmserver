package crmdna.mail2;

import crmdna.client.Client;

import java.util.*;
import java.util.regex.Pattern;

import static crmdna.common.AssertUtils.ensureNoNullElement;
import static crmdna.common.OfyService.ofy;

public class SentMailProp {
    public Date sendAttempted;
    public Long memberId;
    public String email;
    public Long mailContentId;

    public String from;
    public Long sendMS;

    // dependents
    public String body;
    public String subject;

    public static void populateDependents(String client, List<SentMailProp> props) {

        // do a bulk get and populate subject and body
        Client.ensureValid(client);

        ensureNoNullElement(props);

        Set<Long> mailContentIds = new HashSet<>();
        for (SentMailProp sentMailProp : props) {
            mailContentIds.add(sentMailProp.mailContentId);
        }

        Map<Long, MailContentEntity> map =
                ofy(client).load().type(MailContentEntity.class).ids(mailContentIds);

        for (SentMailProp sentMailProp : props) {
            if (map.containsKey(sentMailProp.mailContentId)) {
                MailContentEntity mailContentEntity = map.get(sentMailProp.mailContentId);
                sentMailProp.subject = mailContentEntity.subject;

                sentMailProp.body = mailContentEntity.body;
            }
        }
    }
}
