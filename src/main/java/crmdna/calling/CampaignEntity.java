package crmdna.calling;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Created by sathya on 17/8/15.
 */
@Entity @Cache
public class CampaignEntity {
    @Id
    long campaignId;

    @Index long groupId;

    @Index
    String campaignName; //combination of programId and campaignName should be unique

    String displayName;

    @Index
    long programId;

    @Index
    boolean enabled;

    @Index
    int startYYYYMMDD;

    @Index
    int endYYYYMMDD;

    public CampaignProp toProp() {
        CampaignProp prop = new CampaignProp();

        prop.campaignId = campaignId;
        prop.groupId = groupId;
        prop.campaignName = campaignName;
        prop.displayName = displayName;
        prop.programId = programId;
        prop.enabled = enabled;
        prop.startYYYYMMDD = startYYYYMMDD;
        prop.endYYYYMMDD = endYYYYMMDD;

        return prop;
    }
}
