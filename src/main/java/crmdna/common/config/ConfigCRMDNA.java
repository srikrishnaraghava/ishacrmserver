package crmdna.common.config;

import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.user.UserCore;

import static crmdna.common.OfyService.ofyCrmDna;

public class ConfigCRMDNA {

    private static final String DEFAULT_FROM_EMAIL = "sathya.t@ishafoundation.org";

    private static final String DEV_TEAM_MEMBER_1 = "sathya.t@ishafoundation.org";
    private static final String DEV_TEAM_MEMBER_2 = "thulasidhar@gmail.com";

    public static ConfigCRMDNAProp set(String fromEmail, Boolean devMode, String login) {

        if (login == null)
            throw new APIException().status(Status.ERROR_LOGIN_REQUIRED).message("Please login");

        if (!UserCore.isSuperUser(login))
            throw new APIException().status(Status.ERROR_INSUFFICIENT_PERMISSION).message(
                    "This method can only be called by SUPER_USER");

        ConfigCRMDNAEntity entity = ofyCrmDna().load().type(ConfigCRMDNAEntity.class).id("KEY").now();

        if (null == entity)
            entity = new ConfigCRMDNAEntity();

        if (fromEmail != null) {
            Utils.ensureValidEmail(fromEmail);
            entity.fromEmail = fromEmail;
        }

        if (devMode != null) {
            entity.devMode = devMode;
        }

        ofyCrmDna().save().entity(entity).now();

        return entity.toProp();
    }

    public static ConfigCRMDNAEntity get() {

        ConfigCRMDNAEntity entity = ofyCrmDna().load().type(ConfigCRMDNAEntity.class).id("KEY").now();

        if (entity == null) {
            // save an entry with default values
            entity = new ConfigCRMDNAEntity();
            entity.fromEmail = DEFAULT_FROM_EMAIL;

            entity.devTeamEmails.add(DEV_TEAM_MEMBER_1);
            entity.devTeamEmails.add(DEV_TEAM_MEMBER_2);

            entity.devMode = false;

            ofyCrmDna().save().entity(entity).now();
        }

        return entity;
    }

    public static ConfigCRMDNAProp addOrDeleteDevTeamMember(String email, boolean add, String login) {

        if (!UserCore.isSuperUser(login))
            throw new APIException().status(Status.ERROR_INSUFFICIENT_PERMISSION).message(
                    "This method can be only called by SUPER_USER");

        ConfigCRMDNAEntity entity = get();

        boolean change;
        if (add) {
            Utils.ensureValidEmail(email);
            change = entity.devTeamEmails.add(email);
        } else {
            change = entity.devTeamEmails.remove(email);
        }

        if (change)
            ofyCrmDna().save().entity(entity).now();

        return entity.toProp();
    }
}
