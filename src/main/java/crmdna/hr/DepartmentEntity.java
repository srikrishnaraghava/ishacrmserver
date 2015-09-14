package crmdna.hr;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class DepartmentEntity {
    @Id
    long departmentId;
    String displayName;
    @Index
    String name;

    public DepartmentProp toProp() {
        DepartmentProp prop = new DepartmentProp();
        prop.departmentId = departmentId;
        prop.displayName = displayName;
        prop.name = name;

        return prop;
    }
}
