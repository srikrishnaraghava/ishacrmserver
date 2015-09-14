package crmdna.interaction;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.member.MemberLoader;
import crmdna.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static crmdna.common.OfyService.ofy;


/**
 * Created by sathya on 16/8/15.
 */
public class InteractionScore {
    static InteractionScoreProp incrementBy1(String client, String userEmail, long memberId) {
        Client.ensureValid(client);

        //this is automatically called when a subinteraction is added.
        //hence no privilege check

        long userId = User.safeGet(client, userEmail).toProp(client).userId;

        MemberLoader.safeGet(client, memberId, User.SUPER_USER);

        String key = getKey(userId, memberId);
        InteractionScoreEntity entity = ofy(client).load().type(InteractionScoreEntity.class).id(key).now();
        if (entity == null) {
            entity = new InteractionScoreEntity();
            entity.interactionScore = 0;
            entity.memberId = memberId;
            entity.userId = userId;
            entity.userIdMemberId = key;
        }

        entity.interactionScore++;

        ofy(client).save().entity(entity).now();

        return toProp(entity);
    }

    static String getKey(long userId, long memberId) {
        return userId + "_" + memberId;
    }

    static InteractionScoreProp toProp(InteractionScoreEntity entity) {
        InteractionScoreProp prop = new InteractionScoreProp();
        prop.interactionScore = entity.interactionScore;
        prop.memberId = entity.memberId;
        prop.userId = entity.userId;

        return prop;
    }

    public static List<InteractionScoreProp> get(String client, List<UserMemberProp> userMemberProps) {
        Client.ensureValid(client);

        List<String> keys = new ArrayList<>(userMemberProps.size());
        for (UserMemberProp ump : userMemberProps) {
            String key = getKey(ump.userId, ump.memberId);
            keys.add(key);
        }

        Map<String, InteractionScoreEntity> map = ofy(client).load().type(InteractionScoreEntity.class).ids(keys);

        List<InteractionScoreProp> result = new ArrayList<>();
        for (Map.Entry<String, InteractionScoreEntity> e : map.entrySet()) {
            result.add(toProp(e.getValue()));
        }

        //interaction score is 0 when there is no entry for member, user
        for (String key : keys) {
            if (! map.containsKey(key)) {
                InteractionScoreProp prop = new InteractionScoreProp();
                prop.interactionScore = 0;
                String split[] = key.split(Pattern.quote("_"));
                prop.userId = Integer.parseInt(split[0]);
                prop.memberId = Integer.parseInt(split[1]);

                result.add(prop);
            }
        }

        sortByDescScores(result);

        return result;
    }

    static void sortByDescScores(List<InteractionScoreProp> props) {

        Collections.sort(props, new Comparator<InteractionScoreProp>() {
            @Override
            public int compare(InteractionScoreProp o1, InteractionScoreProp o2) {
                return new Integer(o2.interactionScore).compareTo(new Integer(o1.interactionScore));
            }
        });
    }

    static List<InteractionScoreProp> query(String client, InteractionScoreQueryCondition qc, int maxResults) {
        Client.ensureValid(client);

        Query<InteractionScoreEntity> q = ofy(client).load().type(InteractionScoreEntity.class);

        if (qc.userIds != null && !qc.userIds.isEmpty()) {
            q.filter("userId in", qc.userIds);
        }

        if (qc.memberIds != null && !qc.memberIds.isEmpty()) {
            q.filter("memberId in", qc.memberIds);
        }

        List<Key<InteractionScoreEntity>> keys = q.keys().list();

        if (keys.size() > maxResults) {
            throw new APIException().status(APIResponse.Status.ERROR_OVERFLOW)
                    .message("Query result has " + keys.size() + " entries. Max allowed is " + maxResults);
        }

        Map<Key<InteractionScoreEntity>, InteractionScoreEntity> map = ofy(client).load().keys(keys);

        List<InteractionScoreProp> props = new ArrayList<>();
        for (Map.Entry<Key<InteractionScoreEntity>, InteractionScoreEntity> e : map.entrySet()) {
            props.add(toProp(e.getValue()));
        }

        sortByDescScores(props);

        return props;
    }
}
