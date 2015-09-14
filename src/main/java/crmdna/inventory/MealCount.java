package crmdna.inventory;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.List;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.OfyService.ofy;

public class MealCount {
    public static MealCountProp setCount(String client, int yyyymmdd, Integer breakfastCount,
                                         Integer lunchCount, Integer dinnerCount, String login) {

        Client.ensureValid(client);

        DateUtils.ensureFormatYYYYMMDD(yyyymmdd);

        ensure((breakfastCount != null) || (lunchCount != null) || (dinnerCount != null),
                "Either breakfastCount or lunchCount or dinnerCount should be specified");

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_MEAL_COUNT);

        MealCountEntity entity = ofy(client).load().type(MealCountEntity.class).id(yyyymmdd).now();
        if (entity == null) {
            entity = new MealCountEntity();
            entity.yyyymmdd = yyyymmdd;
        }

        if (breakfastCount != null) {
            ensure(breakfastCount >= 0, "Invalid breakfastCount [" + breakfastCount + "]");
            entity.breakfastCount = breakfastCount;
        }

        if (lunchCount != null) {
            ensure(lunchCount >= 0, "Invalid lunchCount [" + lunchCount + "]");
            entity.lunchCount = lunchCount;
        }

        if (dinnerCount != null) {
            ensure(dinnerCount >= 0, "Invalid dinnerCount [" + dinnerCount + "]");
            entity.dinnerCount = dinnerCount;
        }

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static List<MealCountEntity> query(String client, Integer startYYYYMMDD,
                                              Integer endYYYYMMDD) {

        Client.ensureValid(client);

        ensure((startYYYYMMDD != null) || (endYYYYMMDD != null),
                "Both startYYYYMMDD and endYYYYMMDD are null");

        if (startYYYYMMDD != null)
            DateUtils.ensureFormatYYYYMMDD(startYYYYMMDD);

        if (endYYYYMMDD != null)
            DateUtils.ensureFormatYYYYMMDD(endYYYYMMDD);

        if ((startYYYYMMDD != null) && (endYYYYMMDD != null)) {
            ensure(endYYYYMMDD >= startYYYYMMDD, "End date [" + endYYYYMMDD
                    + "] is less than start date [" + startYYYYMMDD + "]");
        }

        Query<MealCountEntity> q = ofy(client).load().type(MealCountEntity.class);

        if (startYYYYMMDD != null) {
            Key<MealCountEntity> startKey = Key.create(MealCountEntity.class, startYYYYMMDD);
            q = q.filterKey(">=", startKey);
        }

        if (endYYYYMMDD != null) {
            Key<MealCountEntity> endKey = Key.create(MealCountEntity.class, endYYYYMMDD);
            q = q.filterKey("<=", endKey);
        }

        q = q.orderKey(true);

        return q.list();
    }

    public enum Meal {
        BREAKFAST, LUNCH, DINNER
    }
}
