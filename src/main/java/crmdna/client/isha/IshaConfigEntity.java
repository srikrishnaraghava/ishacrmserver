package crmdna.client.isha;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import crmdna.client.isha.IshaConfig.IshaConfigProp;

import java.util.HashSet;
import java.util.Set;

@Entity
@Cache
public class IshaConfigEntity {
    @Id
    public String key = "KEY";

    Set<Long> sathsangPracticesIds = new HashSet<>();

    IshaConfigProp toProp() {
        IshaConfigProp prop = new IshaConfigProp();
        prop.sathsangPracticeIds = sathsangPracticesIds;
        return prop;
    }
}
