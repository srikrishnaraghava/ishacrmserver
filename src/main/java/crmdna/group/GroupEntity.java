package crmdna.group;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.group.Group.GroupProp;

import java.util.HashMap;
import java.util.Map;

@Entity
@Cache
public class GroupEntity {
    @Index
    public String name;
    @Id
    long groupId;
    String displayName;

    // paypal details
    String paypalApiLogin;
    String paypalApiPwd;
    String paypalApiSecret;
    boolean paypalApiSandbox = false;
    boolean paypalApiDisable = false;

    // mandrill
    String mandrillApiKey;

    Map<String, String> allowedFromEmailVsName = new HashMap<>();
    String contactEmail;
    String contactName;

    String registrationConfirmationEmailTemplate;
    String registrationReminderEmailTemplate;

    public GroupProp toProp() {
        GroupProp prop = new GroupProp();
        prop.groupId = groupId;
        prop.name = name;
        prop.displayName = displayName;

        return prop;
    }
}
