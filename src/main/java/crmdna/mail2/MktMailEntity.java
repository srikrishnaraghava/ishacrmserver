package crmdna.mail2;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.HashSet;
import java.util.Set;

@Entity
@Cache
public class MktMailEntity {
    @Id
    long mktMailId;

    @Index
    String name;
    String displayName;

    @Index
    long programId;

    @Index
    Set<Long> mailMessageIds = new HashSet<>();
}
