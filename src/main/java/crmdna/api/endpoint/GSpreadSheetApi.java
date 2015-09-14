package crmdna.api.endpoint;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.server.spi.config.Api;
import com.google.appengine.api.users.User;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.gspreadsheet.GSpreadSheet;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Api(name = "developersOnly")
public class GSpreadSheetApi {

    public APIResponse getPublishedSpreadsheetContents(@Named("spreadSheetKey") String gsKey,
                                                       @Nullable @Named("numLinesExclHeaderDefault2500") Integer numLinesExclHeader,
                                                       @Nullable @Named("showStackTraceIfErrorDefaultFalse") Boolean showStackTrace,
                                                       HttpServletRequest req) {

        try {
            if (numLinesExclHeader == null)
                numLinesExclHeader = 2500;

            List<Map<String, String>> listOfMap =
                    GSpreadSheet.getPublishedSpreasheetAsListOfMap(gsKey, numLinesExclHeader);

            return new APIResponse().status(Status.SUCCESS).object(listOfMap);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    public APIResponse readSpreadsheetContents(@Named("spreadSheetKey") String gsKey,
                                               HttpServletRequest req, User user) {

        try {

            SpreadsheetService service = new SpreadsheetService("MySpreadsheetIntegration-v1");
            URL metafeedUrl =
                    new URL(
                            "https://spreadsheets.google.com/feeds/spreadsheets/1ce55J6l5IjXCOVfFklL9i6c0kYOHu-p9rabM9YdpBTg");

            String authString = req.getHeader("Authorization");
            String strBearer = "Bearer ";
            String accessToken = authString.substring(strBearer.length());

            System.out.println(accessToken);

            GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
            service.setOAuth2Credentials(credential);

            SpreadsheetEntry spreadsheet = service.getEntry(metafeedUrl, SpreadsheetEntry.class);
            URL listFeedUrl = ((WorksheetEntry) spreadsheet.getWorksheets().get(0)).getListFeedUrl();

            // Print entries
            ListFeed feed = (ListFeed) service.getFeed(listFeedUrl, ListFeed.class);
            for (ListEntry entry : feed.getEntries()) {
                System.out.println("new row");
                for (String tag : entry.getCustomElements().getTags()) {
                    System.out.println("     " + tag + ": " + entry.getCustomElements().getValue(tag));
                }
            }

            // // Define the URL to request. This should never change.
            // URL SPREADSHEET_FEED_URL =
            // new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
            //
            // // Make a request to the API and get all spreadsheets.
            // SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
            // List<SpreadsheetEntry> spreadsheets = feed.getEntries();
            //
            // // Iterate through all of the spreadsheets returned
            // for (SpreadsheetEntry spreadsheet : spreadsheets) {
            // // Print the title of this spreadsheet to the screen
            // System.out.println(spreadsheet.getTitle().getPlainText());
            // }

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, true, new RequestInfo().req(req));
        }

    }
}
