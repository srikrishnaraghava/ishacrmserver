package crmdna.interaction;

import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Entity
@Cache
public class InteractionEntity {
    @Id
    long interactionId;

    @Serialize(zip = true)
    Map<Long, String> subInteractions = new HashMap<>();

    // key is nano seconds, value is sub interaction content
    // map is always descending sorted by key

    @Index
    long ms;

    @Index
    String interactionType;

    @Index
    long userId;

    String user; // user name

    @Index
    long memberId;

    @Index(IfNotNull.class)
    Long campaignId;

    public InteractionProp toProp() {
        InteractionProp interactionProp = new InteractionProp();

        interactionProp.interactionId = interactionId;
        interactionProp.timestamp = ms;

        for (Long subinteractionId : subInteractions.keySet()) {
            SubInteractionProp subInteractionProp = new SubInteractionProp();
            subInteractionProp.subInteractionId = subinteractionId;
            subInteractionProp.content = subInteractions.get(subinteractionId);
            subInteractionProp.timestamp = subinteractionId / 1000;
            interactionProp.subInteractionProps.add(subInteractionProp);
        }
        Collections.sort(interactionProp.subInteractionProps);

        interactionProp.interactionType = interactionType;
        interactionProp.user = user;
        interactionProp.memberId = memberId;
        interactionProp.campaignId = campaignId;

        return interactionProp;
    }
}
