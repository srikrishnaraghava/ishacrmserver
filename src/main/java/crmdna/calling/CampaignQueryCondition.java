package crmdna.calling;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sathya on 26/8/15.
 */
public class CampaignQueryCondition {

    public Set<Long> groupIds = new HashSet<>();
    public Set<Long> programIds = new HashSet<>();
    public Boolean enabled;
    public Integer endDateGreaterThanYYYYMMDD;
}
