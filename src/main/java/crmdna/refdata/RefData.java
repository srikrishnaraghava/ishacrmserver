package crmdna.refdata;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static crmdna.common.AssertUtils.ensureNotNull;

public class RefData {
    public static String getCountryAlias(String country) {
        ensureNotNull(country);

        country = country.toLowerCase().replaceAll(Pattern.quote(" "), "");
        Map<String, String> aliasMap = new HashMap<>();

        aliasMap.put("sg", "singapore");
        aliasMap.put("sgp", "singapore");
        aliasMap.put("hk", "hongkong");
        aliasMap.put("my", "malaysia");
        aliasMap.put("qa", "qatar");
        aliasMap.put("th", "thailand");
        aliasMap.put("au", "australia");
        aliasMap.put("jp", "japan");
        aliasMap.put("us", "usa");
        aliasMap.put("phillipines", "philippines");
        aliasMap.put("philipines", "philippines");
        aliasMap.put("phillippines", "philippines");
        aliasMap.put("indonasia", "indonesia");
        aliasMap.put("brunei darussalan", "brunei");

        if (aliasMap.containsKey(country))
            return aliasMap.get(country);

        return country;
    }

    public static CountryProp getCountryProp(String country) {
        ensureNotNull(country);

        country = country.toLowerCase().replaceAll(Pattern.quote(" "), "");
        country = getCountryAlias(country);

        Map<String, CountryProp> map = getCountryRefDataMap();

        if (map.containsKey(country))
            return map.get(country);

        return null;
    }

    private static Map<String, CountryProp> getCountryRefDataMap() {
        Map<String, CountryProp> map = new HashMap<>();

        CountryProp prop = new CountryProp();
        // singapore
        prop.isdCode = "+65";
        prop.numDigitsWOCountryCode = 8;
        prop.mobileRegex = "^[+][6][5][89]\\d{7}$";
        prop.landlineRegex = "^[+][6][5][63]\\d{7}$";
        prop.messageIfError = "Singapore phone no should start with +656 or +653 or +659 or +658 and should have 11 digits including +65";

        map.put("singapore", prop);

        // germany
        prop = new CountryProp();
        prop.isdCode = "+49";
        prop.mobileRegex = "^[+][4][9]\\d{7,20}";
        prop.landlineRegex = "^[+][4][9]\\d{7,20}$";
        prop.messageIfError = "Germany phone no should start with +49";

        map.put("germany", prop);

        // malaysia
        prop = new CountryProp();
        prop.isdCode = "+60";
        prop.mobileRegex = "^[+][6][0]\\d{7,20}";
        prop.landlineRegex = "^[+][6][0]\\d{7,20}$";
        prop.messageIfError = "Malaysia phone no should start with +60";

        map.put("malaysia", prop);

        // japan
        prop = new CountryProp();
        prop.isdCode = "+81";
        prop.mobileRegex = "^[+][8][1]\\d{7,20}";
        prop.landlineRegex = "^[+][8][1]\\d{7,20}$";
        prop.messageIfError = "Japan phone no should start with +81";

        map.put("japan", prop);

        // new zealand
        prop = new CountryProp();
        prop.isdCode = "+64";
        prop.mobileRegex = "^[+][6][4]\\d{7,20}";
        prop.landlineRegex = "^[+][6][4]\\d{7,20}$";
        prop.messageIfError = "New Zealand phone no should start with +81";

        map.put("newzealand", prop);

        // norway
        prop = new CountryProp();
        prop.isdCode = "+47";
        prop.mobileRegex = "^[+][4][7]\\d{7,20}";
        prop.landlineRegex = "^[+][4][7]\\d{7,20}$";
        prop.messageIfError = "Norway phone no should start with +47";

        map.put("norway", prop);

        // qatar
        prop = new CountryProp();
        prop.isdCode = "+974";
        prop.mobileRegex = "^[+][9][7][4]\\d{7,20}";
        prop.landlineRegex = "^[+][9][7][4]\\d{7,20}$";
        prop.messageIfError = "Qatar phone no should start with +974";

        map.put("qatar", prop);

        // hongkong
        prop = new CountryProp();
        prop.isdCode = "+852";
        prop.mobileRegex = "^[+][8][5][2]\\d{7,20}";
        prop.landlineRegex = "^[+][8][5][2]\\d{7,20}$";
        prop.messageIfError = "Hong kong phone no should start with +852";

        map.put("hongkong", prop);

        // canada
        prop = new CountryProp();
        prop.isdCode = "+1";
        prop.mobileRegex = "^[+][1]\\d{7,20}";
        prop.landlineRegex = "^[+][1]\\d{7,20}$";
        prop.messageIfError = "Canada phone no should start with +1";

        map.put("canada", prop);

        // thailand
        prop = new CountryProp();
        prop.isdCode = "+66";
        prop.mobileRegex = "^[+][6][6]\\d{7,20}";
        prop.landlineRegex = "^[+][6][6]\\d{7,20}$";
        prop.messageIfError = "Thailand phone no should start with +66";

        map.put("thailand", prop);

        // australia
        prop = new CountryProp();
        prop.isdCode = "+61";
        prop.mobileRegex = "^[+][6][1]\\d{7,20}";
        prop.landlineRegex = "^[+][6][1]\\d{7,20}$";
        prop.messageIfError = "Australia phone no should start with +61";

        map.put("australia", prop);

        // canada
        prop = new CountryProp();
        prop.isdCode = "+1";
        prop.mobileRegex = "^[+][1]\\d{7,20}";
        prop.landlineRegex = "^[+][1]\\d{7,20}$";
        prop.messageIfError = "USA phone no should start with +1";

        map.put("usa", prop);

        // china
        prop = new CountryProp();
        prop.isdCode = "+86";
        prop.mobileRegex = "^[+][8][6]\\d{7,20}";
        prop.landlineRegex = "^[+][8][6]\\d{7,20}";
        prop.messageIfError = "China phone no should start with +86";

        map.put("china", prop);

        // philippines
        prop = new CountryProp();
        prop.isdCode = "+63";
        prop.mobileRegex = "^[+][6][3]\\d{7,20}";
        prop.landlineRegex = "^[+][6][3]\\d{7,20}";
        prop.messageIfError = "Philippines phone no should start with +86";

        map.put("philippines", prop);

        // india
        prop = new CountryProp();
        prop.isdCode = "+91";
        prop.numDigitsWOCountryCode = 10;
        prop.mobileRegex = "^[+][9][1]\\d{10}";
        prop.landlineRegex = "^[+][9][1]\\d{10}";
        prop.messageIfError = "India phone no should start with +91 and should have 10 digits excluding +91";

        map.put("india", prop);

        // sri lanka
        prop = new CountryProp();
        prop.isdCode = "+94";
        prop.mobileRegex = "^[+][9][4]\\d{7,20}";
        prop.landlineRegex = "^[+][9][4]\\d{7,20}";
        prop.messageIfError = "Sri Lanka phone no should start with +94";

        map.put("srilanka", prop);

        // portugal
        prop = new CountryProp();
        prop.isdCode = "+351";
        prop.mobileRegex = "^[+][3][5][1]\\d{7,20}";
        prop.landlineRegex = "^[+][3][5][1]\\d{7,20}";
        prop.messageIfError = "Portugal phone no should start with +351";

        map.put("portugal", prop);

        // indonesia
        prop = new CountryProp();
        prop.isdCode = "+62";
        prop.mobileRegex = "^[+][6][2]\\d{7,20}";
        prop.landlineRegex = "^[+][6][2]\\d{7,20}";
        prop.messageIfError = "Indonesia phone no should start with +62";

        map.put("indonesia", prop);

        // taiwan
        prop = new CountryProp();
        prop.isdCode = "+886";
        prop.mobileRegex = "^[+][8][8][6]\\d{7,20}";
        prop.landlineRegex = "^[+][8][8][6]\\d{7,20}";
        prop.messageIfError = "Taiwan phone no should start with +886";

        map.put("taiwan", prop);

        // slovakia
        prop = new CountryProp();
        prop.isdCode = "+421";
        prop.mobileRegex = "^[+][4][2][1]\\d{7,20}";
        prop.landlineRegex = "^[+][4][2][1]\\d{7,20}";
        prop.messageIfError = "Slovakia phone no should start with +421";

        map.put("slovakia", prop);

        // bangladesh
        prop = new CountryProp();
        prop.isdCode = "+880";
        prop.mobileRegex = "^[+][8][8][0]\\d{7,20}";
        prop.landlineRegex = "^[+][8][8][0]\\d{7,20}";
        prop.messageIfError = "Bangladesh phone no should start with +880";

        map.put("bangladesh", prop);

        // vietnam
        prop = new CountryProp();
        prop.isdCode = "+84";
        prop.mobileRegex = "^[+][8][4]\\d{7,20}";
        prop.landlineRegex = "^[+][8][4]\\d{7,20}";
        prop.messageIfError = "Vietnam phone no should start with +84";

        map.put("vietnam", prop);

        // botswana
        prop = new CountryProp();
        prop.isdCode = "+267";
        prop.mobileRegex = "^[+][2][6][7]\\d{7,20}";
        prop.landlineRegex = "^[+][2][6][7]\\d{7,20}";
        prop.messageIfError = "Botswana phone no should start with +267";

        map.put("botswana", prop);

        // brunei
        prop = new CountryProp();
        prop.isdCode = "+673";
        prop.mobileRegex = "^[+][6][7][3]\\d{7,20}";
        prop.landlineRegex = "^[+][6][7][3]\\d{7,20}";
        prop.messageIfError = "Brunei phone no should start with +673";

        map.put("botswana", prop);

        return map;
    }
}
