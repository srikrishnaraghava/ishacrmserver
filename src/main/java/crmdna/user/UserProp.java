package crmdna.user;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class UserProp {
    public String email;

    public long groupId;
    public long userId;

    public Set<String> clientLevelPrivileges = new TreeSet<>();
    public Map<String, Set<String>> groupLevelPrivileges = new TreeMap<>();

    public TreeSet<String> apps = new TreeSet<>();

    // Dependents
    public String groupName;
}
