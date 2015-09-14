package crmdna.programtype;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.practice.Practice;
import crmdna.practice.PracticeEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Entity
@Cache
public class ProgramTypeEntity {
    @Id
    Long programTypeId;
    @Index
    String name;
    String displayName;

    @Index
    Set<Long> practiceIds = new HashSet<>();

    public ProgramTypeProp toProp(String client) {
        ProgramTypeProp prop = new ProgramTypeProp();
        prop.programTypeId = programTypeId;
        prop.name = name;
        prop.displayName = displayName;

        Map<Long, PracticeEntity> map = Practice.getEntities(client, practiceIds);

        for (Entry<Long, PracticeEntity> entry : map.entrySet()) {
            prop.practiceProps.add(entry.getValue().toProp());
        }

        Collections.sort(prop.practiceProps);

        return prop;
    }
}
