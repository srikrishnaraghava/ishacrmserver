package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "program")
public class VenueApi {
    @ApiMethod(path = "createVenue", httpMethod = HttpMethod.POST)
    public APIResponse createVenue(@Named("client") String client,
        @Named("display_name") String displayName, @Named("short_name") String shortName,
        @Named("address") String address, @Named("group_id") long groupId,
        @Nullable @Named("showStackTrace") Boolean showStackTrace,
        HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            VenueProp prop = Venue.create(client, displayName, shortName, address, groupId, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getAllVenues", httpMethod = HttpMethod.GET)
    public APIResponse getAllVenues(@Named("client") String client,
                                    @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (client == null)
            client = "isha";

        try {

            Client.ensureValid(client);
            List<VenueProp> props = Venue.getAll(client);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "updateVenue", httpMethod = HttpMethod.GET)
    public APIResponse updateVenue(@Named("client") String client, @Named("venue_id") long venueId,
                                   @Nullable @Named("new_display_name") String newDisplayName,
                                   @Nullable @Named("new_short_name") String newShortName,
                                   @Nullable @Named("new_address") String newAddress,
                                   @Nullable @Named("group_id") Long newGroupId,
                                   @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            VenueProp prop = Venue.update(client, venueId, newDisplayName, newShortName, newAddress,
                newGroupId, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteVenue", httpMethod = HttpMethod.GET)
    public APIResponse deleteVenue(@Named("client") String client, @Named("venue_id") long venueId,
                                   @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);
            Venue.delete(client, venueId, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object("Venue [" + venueId + "] deleted");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
