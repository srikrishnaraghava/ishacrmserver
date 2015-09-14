package crmdna.practice;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.practice.Practice.PracticeProp;

@Entity
@Cache
public class PracticeEntity {
    @Index
    public String name;
    @Id
    long practiceId;
    String displayName;

    public PracticeProp toProp() {
        PracticeProp prop = new PracticeProp();
        prop.practiceId = practiceId;
        prop.name = name;
        prop.displayName = displayName;

        return prop;
    }
}
