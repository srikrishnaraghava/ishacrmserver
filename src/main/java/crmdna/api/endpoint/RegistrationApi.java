package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.registration.*;
import crmdna.registration.Registration.RegistrationStatus;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

@Api(name = "registration", description = "Registration (Alpha)", clientIds = {
        "429804891913.apps.googleusercontent.com",
        com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID})
public class RegistrationApi {
    @ApiMethod(path = "query", httpMethod = HttpMethod.GET)
    public APIResponse query(@Named("client") String client,
                             @Nullable @Named("programId") Long programId, @Nullable @Named("searchStr") String searchStr,
                             @Nullable @Named("status") RegistrationStatus status,
                             @Nullable @Named("sortByFirstName") Boolean sortByFirstName,
                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {
            RegistrationQueryCondition qc = new RegistrationQueryCondition();
            qc.programId = programId;
            qc.searchStr = searchStr;
            qc.status = status;

            if (sortByFirstName == null)
                sortByFirstName = false;
            qc.sortByFirstName = sortByFirstName;

            login = Utils.getLoginEmail(user);
            List<RegistrationProp> props = Registration.query(client, qc, login);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "queryByTransactionId", httpMethod = HttpMethod.GET)
    public APIResponse queryByTransactionId(@Named("client") String client,
                                            @Named("transactionId") String transactionId,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        try {
            if (null == client)
                client = "isha";

            RegistrationProp registrationProp = Registration.queryByTransactionId(client, transactionId);

            return new APIResponse().status(Status.SUCCESS).object(registrationProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "getByTransactionId", httpMethod = HttpMethod.GET)
    public APIResponse getByTransactionId(@Named("client") String client,
                                          @Named("transactionId") String transactionId,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        try {
            if (null == client)
                client = "isha";

            RegistrationProp registrationProp = Registration.getByTransactionId(client, transactionId);

            return new APIResponse().status(Status.SUCCESS).object(registrationProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "queryRegistrationStatus", httpMethod = HttpMethod.GET)
    public APIResponse queryRegistrationStatus(@Named("client") String client,
                                               @Named("programId") long programId, @Named("email") String email,
                                               @Named("firstName") String firstName, @Nullable @Named("lastName") String lastName,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        try {
            if (null == client)
                client = "isha";

            RegistrationStatusProp registrationStatusProp =
                    Registration.queryRegistrationStatus(client, programId, email, firstName, lastName);

            return new APIResponse().status(Status.SUCCESS).object(registrationStatusProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "getSummary", httpMethod = HttpMethod.GET)
    public APIResponse getSummary(@Named("client") String client,
                                  @Named("programId") long programId,
                                  @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            RegistrationSummaryProp registrationSummaryProp =
                    Registration.getSummary(client, programId, login);

            return new APIResponse().status(Status.SUCCESS).object(registrationSummaryProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "createDiscountCode", httpMethod = HttpMethod.GET)
    public APIResponse createDiscountCode(@Named("client") String client,
                                  @Named("programTypeIds") Set<Long> programTypeIds,
                                  @Named("discountCode") String discountCode,
                                  @Named("validTillYYYYMMDD") Integer validTillYYYYMMDD,
                                  @Nullable @Named("percentage") Double percentage,
                                  @Nullable @Named("amount") Double amount,
                                  @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                  HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);

            DiscountProp prop =
                    Discount.createDiscountCode(client, discountCode, programTypeIds, validTillYYYYMMDD, percentage, amount, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "updateDiscountCode", httpMethod = HttpMethod.GET)
    public APIResponse updateDiscountCode(@Named("client") String client,
                                          @Nullable @Named("programTypeIds") Set<Long> newProgramTypeIds,
                                          @Nullable @Named("discountCode") String newDiscountCode,
                                          @Nullable @Named("validTillYYYYMMDD") Integer newValidTillYYYYMMDD,
                                          @Nullable @Named("percentage") Double newPercentage,
                                          @Nullable @Named("amount") Double newAmount,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                          HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);

            DiscountProp prop = Discount.updateDiscountCode(client, newDiscountCode, newProgramTypeIds,
                    newValidTillYYYYMMDD, newPercentage, newAmount, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }
}
