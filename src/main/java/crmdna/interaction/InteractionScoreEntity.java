package crmdna.interaction;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class InteractionScoreEntity {
    @Id
    String userIdMemberId;
    int interactionScore;

    @Index
    long userId;

    @Index
    long memberId;
}
