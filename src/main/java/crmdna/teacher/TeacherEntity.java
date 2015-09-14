package crmdna.teacher;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.teacher.Teacher.TeacherProp;

@Entity
@Cache
public class TeacherEntity {
    @Id
    long teacherId;

    @Index
    String firstName;

    @Index
    String lastName;

    @Index
    String email;

    @Index
    long groupId;

    public TeacherProp toProp() {
        TeacherProp prop = new TeacherProp();
        prop.firstName = firstName;
        prop.lastName = lastName;
        prop.teacherId = teacherId;
        prop.email = email;
        prop.groupId = groupId;

        return prop;
    }
}
