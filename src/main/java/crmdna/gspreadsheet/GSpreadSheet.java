package crmdna.gspreadsheet;

import com.google.gson.*;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class GSpreadSheet {

    final static int MAX_LINES = 25000;

    public static List<Map<String, String>> getPublishedSpreasheetAsListOfMap(String gsKey,
                                                                              int numLinesExclHeader) throws IOException {

        if (numLinesExclHeader > MAX_LINES)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Num lines to read (excl. header) [" + numLinesExclHeader + "] cannot be greater than ["
                            + MAX_LINES + "]");

        String jsonFeedURL = getJSONFeedURL(gsKey);

        String json = Utils.readDataFromURL(jsonFeedURL);

        List<Map<String, String>> listOfMap = getListOfMap(json, numLinesExclHeader);

        return listOfMap;
    }

    static List<Map<String, String>> getListOfMap(String json, int maxLines) {

        Set<String> keysToIgnore = new HashSet<>();
        keysToIgnore.add("id");
        keysToIgnore.add("updated");
        keysToIgnore.add("category");
        keysToIgnore.add("title");
        keysToIgnore.add("content");
        keysToIgnore.add("link");

        JsonParser parser = new JsonParser();

        JsonObject jsonObject;

        try {
            jsonObject = parser.parse(json).getAsJsonObject().getAsJsonObject("feed");
        } catch (JsonSyntaxException jse) {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Unable to read spreadsheet data. "
                            + "Please check if spreadhsheet key is valid and the spreadsheet is published.");
        }

        if (null == jsonObject) {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid json. Cannot find element [feed]");
        }

        JsonArray rows = jsonObject.getAsJsonArray("entry");

        List<Map<String, String>> list = new ArrayList<>();

        int lineNo = 0;
        for (JsonElement row : rows) {
            lineNo++;

            if (lineNo > maxLines)
                break;

            jsonObject = (JsonObject) row;

            Map<String, String> map = new HashMap<>();
            list.add(map);

            for (Entry<String, JsonElement> element : jsonObject.entrySet()) {

                String key = element.getKey();
                if (keysToIgnore.contains(key))
                    continue;

                if (!key.startsWith("gsx$"))
                    throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                            "Invalid column header key [" + key
                                    + "]. Key for column header should start with gsx$");

                key = key.substring(4);

                String value = ((JsonObject) element.getValue()).get("$t").getAsString();

                map.put(key, value);
            }
        }

        return list;
    }

    static String getJSONFeedURL(String gsKey) {
        return "https://spreadsheets.google.com/feeds/list/" + gsKey + "/od6/public/values?alt=json";
    }
}
