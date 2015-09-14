package crmdna.mail2;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.HashSet;
import java.util.Set;

@Entity
public class TagSetEntity {
    @Id
    long tagSetId;

    @Index
    Set<String> tags = new HashSet<>();

    public TagSetProp toProp() {
        TagSetProp prop = new TagSetProp();
        prop.tagSetId = tagSetId;
        prop.tags = tags;

        return prop;
    }
}
