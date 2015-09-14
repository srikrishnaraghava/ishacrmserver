package crmdna.sequence;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class SequenceEntity {
    @Id
    String id;
    long counter = 0;
}