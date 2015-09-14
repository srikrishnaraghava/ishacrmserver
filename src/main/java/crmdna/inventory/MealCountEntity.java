package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Cache
public class MealCountEntity {
    @Id
    long yyyymmdd;

    int breakfastCount;
    int lunchCount;
    int dinnerCount;

    public MealCountProp toProp() {
        MealCountProp prop = new MealCountProp();

        prop.yyyymmdd = (int) yyyymmdd;
        prop.breakfastCount = breakfastCount;
        prop.lunchCount = lunchCount;
        prop.dinnerCount = dinnerCount;

        return prop;
    }
}
