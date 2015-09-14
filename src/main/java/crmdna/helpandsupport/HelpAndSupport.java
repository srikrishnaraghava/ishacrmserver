package crmdna.helpandsupport;

import static crmdna.common.OfyService.ofyCrmDna;

public class HelpAndSupport {

    public static ConfigHelpProp getConfigHelpProp() {
        ConfigHelpEntity entity = ofyCrmDna().load().type(ConfigHelpEntity.class).id("KEY").now();

        if (entity == null) {
            // save default values
            entity = new ConfigHelpEntity();
            ofyCrmDna().save().entity(entity).now();
        }

        return entity.toProp();
    }
}
