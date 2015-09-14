package crmdna.list;

import java.util.Set;

public class ListProp implements Comparable<ListProp> {
    public long listId;
    public String name;
    public String displayName;
    public long groupId;
    public boolean enabled;
    public boolean restricted;

    public Set<Long> practiceIds;
    // dependents
    public String groupName;

    @Override
    public int compareTo(ListProp o) {
        if ((o == null) || (o.name == null) || (name == null)) {
            // should never happen
            return 0;
        }

        return name.compareTo(o.name);
    }
}
