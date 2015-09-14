package crmdna.interaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InteractionQueryCondition {
    public List<Long> memberIds = new ArrayList<>();
    public List<Long> userIds = new ArrayList<>();

    public Set<Long> campaignIds = new HashSet<>();

    public List<String> interactionTypes = new ArrayList<>();

    public Date start;
    public Date end;

    // interactions are always sorted in descending order of time
    // in-memory key filters
    public Integer startIndex;
    public Integer numResults;
}
