package crmdna.registration;

import java.util.Set;

public class DiscountProp {

    public long discountId;
    public String discountCode;
    public Set<Long> programTypeIds;
    public int validTillYYYYMMDD;
    public double amount;
    public double percentage;

    public double discountedAmount;
}
