package crmdna.venue;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.common.Utils;
import crmdna.venue.Venue.VenueProp;

@Entity
@Cache
public class VenueEntity {
    @Id
    long venueId;
    @Index
    String name;
    String displayName;
    String shortName;
    String address;
    @Index
    long groupId;

    public VenueProp toProp() {
        VenueProp prop = new VenueProp();
        prop.venueId = venueId;
        prop.name = name;
        prop.displayName = displayName;
        prop.address = address;
        prop.groupId = groupId;
        prop.shortName = shortName;

        return prop;
    }
}
