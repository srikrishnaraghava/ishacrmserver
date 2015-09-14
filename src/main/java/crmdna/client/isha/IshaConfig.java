package crmdna.client.isha;

import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.practice.Practice;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.Set;

import static crmdna.common.OfyService.ofy;

public class IshaConfig {
    private static final String ISHA = "isha";
    private static final String KEY = "KEY";

    public static IshaConfigProp setSathsangPractices(Set<Long> practiceIds, String login) {

        Client.ensureValid(ISHA);
        User.ensureClientLevelPrivilege(ISHA, login, ClientLevelPrivilege.UPDATE_CUSTOM_CONFIG);

        if ((null == practiceIds) || (practiceIds.size() == 0))
            Utils.throwIncorrectSpecException("practiceIds is null or empty set");

        // validate all practices
        for (Long practiceId : practiceIds) {
            Practice.safeGet(ISHA, practiceId);
        }

        IshaConfigEntity entity = ofy(ISHA).load().type(IshaConfigEntity.class).id(KEY).now();
        if (null == entity) {
            entity = new IshaConfigEntity();
            entity.key = KEY;
        }

        entity.sathsangPracticesIds = practiceIds;
        ofy(ISHA).save().entity(entity).now();

        return entity.toProp();
    }

    public static IshaConfigProp safeGet() {
        Client.ensureValid(ISHA);

        IshaConfigEntity entity = ofy(ISHA).load().type(IshaConfigEntity.class).id(KEY).now();
        if (null == entity)
            Utils.throwNotFoundException("CustomConfig not specified for client [" + ISHA + "]");

        return entity.toProp();
    }

    public static class IshaConfigProp {
        public Set<Long> sathsangPracticeIds;
    }
}
