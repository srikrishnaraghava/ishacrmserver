package crmdna.useractivity;

import crmdna.useractivity.UserActivityCore.UserActivityProp;

import java.util.Date;
import java.util.List;

public class UserActivity {
    public static List<UserActivityProp> getUserActivity(String client,
                                                         long userId, Date start, Date end, String login) {
        return null;
    }

    public static List<UserActivityProp> getActivityForEntity(String client,
                                                              EntityType entityType, long entityId, Date start, Date end,
                                                              String login) {
        return null;
    }

    public static void recordUserActivity(String client, EntityType entityType,
                                          long entityId, UserAction userAction, String change, String login) {
    }

    public enum UserAction {
        VIEW, CREATE, UPDATE, DELETE
    }

    public enum EntityType {
        MEMBER, CENTER
    }
}
