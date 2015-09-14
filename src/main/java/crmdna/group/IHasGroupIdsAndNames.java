package crmdna.group;

import java.util.Set;

public interface IHasGroupIdsAndNames {
    public Set<Long> getGroupIds();

    public void setGroupNames(Set<String> groupNames);
}
