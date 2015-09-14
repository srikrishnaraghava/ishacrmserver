package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.common.DateUtils;
import crmdna.common.DateUtils.FutureDateRange;
import crmdna.common.StopWatch;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.sessionpass.SessionPass;
import crmdna.sessionpass.SessionPassProp;
import crmdna.user.User.ClientLevelPrivilege;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

import static crmdna.common.AssertUtils.ensure;

@Api(name = "member")
public class SessionPassApi {
    @ApiMethod(path = "allocatePasses", httpMethod = HttpMethod.POST)
    public APIResponse allocatePasses(@Named("client") ClientEnum clientEnum,
        @Named("group") ProgramIshaApi.GroupEnum groupEnum,
        @Named("memberIdOrEmail") String memberIdOrEmail, @Named("numSessions") int numSessions,
        @Named("expiry") FutureDateRange dateRange, @Named("amount") double amount,
        @Nullable @Named("currencyDefaultSGD") Currency currency,
        @Nullable @Named("groupOtherIdOrName") String groupOtherIdOrName,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client =
                Utils.removeSpaceUnderscoreBracketAndHyphen(clientEnum.toString().toLowerCase());

        String login = null;

        if (currency == null)
            currency = Currency.SGD;

        try {

            StopWatch stopWatch = StopWatch.createStarted();

            login = Utils.getLoginEmail(user);

            long memberId = 0;
            if (Utils.canParseAsLong(memberIdOrEmail)) {
                memberId = Utils.safeParseAsLong(memberIdOrEmail);
                MemberLoader.safeGet(client, memberId, login);
            } else {
                String email = memberIdOrEmail.toLowerCase();
                Utils.ensureValidEmail(email);
                MemberQueryCondition qc = new MemberQueryCondition(client, 100);
                qc.email = email;
                // TODO: query members with valid account

                List<MemberProp> memberProps = MemberLoader.querySortedProps(qc, login);

                if (memberProps.isEmpty())
                    throw new APIException("No member found with email [" + email + "]")
                            .status(Status.ERROR_RESOURCE_NOT_FOUND);

                if (memberProps.size() > 1)
                    throw new APIException(
                        "Found multiple [" + memberProps.size() + "] members with email [" + email
                            + "]").status(Status.ERROR_RESOURCE_NOT_FOUND);

                memberId = memberProps.get(0).memberId;
            }

            ensure(memberId != 0, "memberId is 0");
            String email = MemberLoader.safeGet(client, memberId, login).toProp().contact.email;
            ensure(Utils.isValidEmailAddress(email), "Email for member id [" + memberId
                    + "] is either not specified or not valid");

            long expiryMS = new Date().getTime() + DateUtils.getMilliSecondsFromDateRange(dateRange);
            long groupId = EndpointUtils.getGroupId(client, groupEnum, groupOtherIdOrName);

            List<SessionPassProp> sessionPassProps = SessionPass
                .allocatePasses(client, groupId, memberId, numSessions, new Date(expiryMS),
                    amount, currency, SessionPass.MANUAL_TRANSACTION_ID, login);

            return new APIResponse().status(Status.SUCCESS).object(sessionPassProps)
                    .message("Allocated [" + numSessions + "] session passes for [" + email + "]")
                    .processingTimeInMS(stopWatch.msElapsed());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "cancelBooking", httpMethod = HttpMethod.POST)
    public APIResponse cancelBooking(@Named("client") ClientEnum clientEnum,
                                     @Named("sessionPassIds") List<Long> sessionPassIds, HttpServletRequest req, User user) {

        String client =
                Utils.removeSpaceUnderscoreBracketAndHyphen(clientEnum.toString().toLowerCase());
        String login = Utils.getLoginEmail(user);

        crmdna.user.User.ensureClientLevelPrivilege(client, login,
                ClientLevelPrivilege.UPDATE_SESSION_PASS);

        try {

            for (long id : sessionPassIds) {
                SessionPass.cancelBooking(client, id);
            }

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, false,
                    new RequestInfo().client(client).req(req).login(login));
        }
    }
}
