package crmdna.list;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.HashSet;
import java.util.Set;

@Entity
@Cache
public class ListEntity {

    @Id
    long listId;

    @Index
    String name;

    String displayName;

    @Index
    long groupId;

    @Index
    boolean enabled;

    @Index
    boolean restricted;

    @Index
    Set<Long> practiceIds = new HashSet<>();

    public ListProp toProp() {
        ListProp prop = new ListProp();
        prop.listId = listId;
        prop.name = name;
        prop.displayName = displayName;
        prop.groupId = groupId;
        prop.enabled = enabled;
        prop.restricted = restricted;
        prop.practiceIds = practiceIds;

        return prop;
    }
}
