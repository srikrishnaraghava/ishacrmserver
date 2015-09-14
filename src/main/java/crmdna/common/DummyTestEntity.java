package crmdna.common;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Cache
public class DummyTestEntity {
    @Id
    public long id;
    public String field1;
}
