package crmdna.common.config;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Set;
import java.util.TreeSet;

@Entity
@Cache
public class ConfigCRMDNAEntity {
    @Id
    String key = "KEY";

    String fromEmail;
    Set<String> devTeamEmails = new TreeSet<>();
    boolean devMode = false;

    public ConfigCRMDNAProp toProp() {
        ConfigCRMDNAProp prop = new ConfigCRMDNAProp();

        prop.fromEmail = fromEmail;
        prop.devTeamEmails = devTeamEmails;
        prop.devMode = devMode;

        return prop;
    }
}
