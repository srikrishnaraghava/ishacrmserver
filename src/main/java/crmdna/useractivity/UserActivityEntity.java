package crmdna.useractivity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.useractivity.UserActivityCore.UserActivityProp;

import java.util.Date;

@Entity
@Cache
public class UserActivityEntity {
    @Id
    Long userActivityId;
    @Index
    long userId;
    @Index
    String entityType;
    @Index
    long entityId;
    @Index
    String userAction;
    @Index
    Date timestamp;
    String change;

    UserActivityProp toProp() {
        UserActivityProp prop = new UserActivityProp();

        prop.userActivityId = userActivityId;
        prop.userId = userId;
        prop.entityType = entityType;
        prop.entityId = entityId;
        prop.userAction = userAction;
        prop.timestamp = timestamp;
        prop.change = change;

        return prop;
    }
}
