package crmdna.mail2;

import crmdna.common.Utils;
import crmdna.mail2.Mail.MetaData;


public class MandrillEventProp {
    public long ts;
    public String event;
    public String url;
    public String ip;
    public String user_agent;

    public MandrillLocationProp location;
    public MandrillUserAgentParsedProp user_agent_parsed;
    public String _id;

    public MandrillMessageProp msg;

    // to capture sync events
    public String type;
    public String action;

    public String getClient() {
        if (msg == null)
            return null;

        if (msg.metadata == null)
            return null;

        if (!msg.metadata.containsKey(MetaData.CLIENT.toString()))
            return null;

        return msg.metadata.get(MetaData.CLIENT.toString());
    }

    public Long getMailId() {
        if (msg == null)
            return null;

        if (msg.metadata == null)
            return null;

        if (!msg.metadata.containsKey(MetaData.MAIL_ID.toString()))
            return null;

        String mailId = msg.metadata.get(MetaData.MAIL_ID.toString());

        if (!Utils.canParseAsLong(mailId))
            return null;

        return Utils.safeParseAsLong(mailId);
    }
}
