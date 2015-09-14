package crmdna.helpandsupport;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class ConfigHelpEntity {
    @Id
    String key = "KEY";
    String fromEmail = "Outgoing emails will have [fromEmail] as the sender name and sender email address. "
            + "The email address specified in [fromEmail] should be a valid administrator for the GAE application. "
            + "To set a user as an administrator give the email atleast viewer rights in GAE administration console.";

    public ConfigHelpProp toProp() {
        ConfigHelpProp prop = new ConfigHelpProp();
        prop.fromEmail = fromEmail;

        return prop;
    }
}
