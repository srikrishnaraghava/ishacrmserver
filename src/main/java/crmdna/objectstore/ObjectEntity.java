package crmdna.objectstore;

import com.googlecode.objectify.annotation.*;

@Entity
@Cache
public class ObjectEntity {
    @Id
    Long objectId;

    @Serialize(zip = true)
    Object object;

    @Index
    long expiryMS;
}
