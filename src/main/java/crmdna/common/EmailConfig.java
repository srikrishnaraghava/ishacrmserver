package crmdna.common;

import java.util.HashMap;
import java.util.Map;

public class EmailConfig {
    public final static String TEXT_API_KEY_MASKED = "Api key masked";

    public String mandrillApiKey;

    public Map<String, String> allowedFromEmailVsName = new HashMap<>();
    public String contactEmail;
    public String contactName;
}
