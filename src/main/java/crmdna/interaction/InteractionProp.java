package crmdna.interaction;

import java.util.ArrayList;
import java.util.List;

public class InteractionProp {
    public long interactionId;
    public long timestamp; //epoch milliseconds

    public List<SubInteractionProp> subInteractionProps = new ArrayList<>();

    public String interactionType;

    public long memberId;
    public String user;

    public Long campaignId;
}
