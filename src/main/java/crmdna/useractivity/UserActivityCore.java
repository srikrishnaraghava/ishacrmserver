package crmdna.useractivity;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static crmdna.common.OfyService.ofy;

public class UserActivityCore {
    static List<UserActivityProp> getUserActivity(String client, long userId,
                                                  Date start, Date end) {

        Client.ensureValid(client);

        Query<UserActivityEntity> query = ofy(client).load()
                .type(UserActivityEntity.class).filter("userId", userId);

        if (null != start)
            query = query.filter("timestamp >=", start);

        if (null != end)
            query = query.filter("timestamp <=", end);

        query = query.order("-timestamp");

        return getQueryResult(query);
    }

    private static List<UserActivityProp> getQueryResult(
            Query<UserActivityEntity> query) {
        List<UserActivityEntity> entities = query.list();

        List<UserActivityProp> props = new ArrayList<>();
        for (UserActivityEntity entity : entities)
            props.add(entity.toProp());

        return props;
    }

    static List<UserActivityProp> getEntityActivity(String client,
                                                    String entityType, long entityId, Date start, Date end) {

        Client.ensureValid(client);

        Query<UserActivityEntity> query = ofy(client).load()
                .type(UserActivityEntity.class)
                .filter("entityType", entityType).filter("entityId", entityId);

        if (null != start)
            query = query.filter("timestamp >=", start);

        if (null != end)
            query = query.filter("timestamp <=", end);

        query = query.order("-timestamp");

        return getQueryResult(query);
    }

    static void recordUserActivity(String client, String entityType,
                                   long entityId, String userAction, String change, long userId) {

        Client.ensureValid(client);

        UserActivityEntity userActivityEntity = new UserActivityEntity();
        userActivityEntity.entityId = entityId;
        userActivityEntity.entityType = entityType.toUpperCase();
        userActivityEntity.userAction = userAction.toUpperCase();
        userActivityEntity.userId = userId;
        userActivityEntity.change = change;
        userActivityEntity.timestamp = new Date();

        ofy(client).save().entity(userActivityEntity);
    }

    public static class UserActivityProp {
        public long userActivityId;
        public long userId;
        public String entityType;
        public long entityId;
        public String userAction;
        public Date timestamp;
        String change;
    }
}
