package crmdna.registration;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.HashSet;
import java.util.Set;

@Entity
@Cache
public class DiscountEntity {
    @Id
    long discountId;

    @Index
    String discountCode;

    @Index
    Set<Long> programTypeIds;

    int validTillYYYYMMDD;

    double percentage;
    double amount;

    public DiscountProp toProp() {
        DiscountProp prop = new DiscountProp();

        prop.discountId = discountId;
        prop.discountCode = discountCode;
        prop.programTypeIds = programTypeIds;
        prop.validTillYYYYMMDD = validTillYYYYMMDD;
        prop.amount = amount;
        prop.percentage = percentage;

        return prop;
    }
}
