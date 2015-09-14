package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.DateUtils.DateRange;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.payment2.Payment;
import crmdna.payment2.Payment.PaymentType;
import crmdna.payment2.PaymentEntity;
import crmdna.payment2.PaymentProp;
import crmdna.payment2.PaymentQueryCondition;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Api(name = "payment")
public class PaymentApi {
    @ApiMethod(path = "recordCashOrChequePayment", httpMethod = HttpMethod.POST)
    public APIResponse recordCashOrChequePayment(@Named("client") ClientEnum clientEnum,
                                                 @Named("amount") double amount, @Nullable @Named("currencyDefaultSGD") Currency currency,
                                                 @Nullable @Named("paymentTypeDefaultCash") PaymentType paymentType,
                                                 @Nullable @Named("collectedByDefaultLoggedInUser") String collectedBy,
                                                 @Nullable @Named("chequeNo") String chequeNo, @Nullable @Named("bank") String bank,
                                                 @Nullable @Named("tags") Set<String> tags,
                                                 @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client =
                Utils.removeSpaceUnderscoreBracketAndHyphen(clientEnum.toString().toLowerCase());

        if (currency == null)
            currency = Currency.SGD;

        if (paymentType == null)
            paymentType = PaymentType.CASH;

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);

            if (collectedBy == null)
                collectedBy = login;

            PaymentProp paymentProp = new PaymentProp();
            paymentProp.amount = amount;
            paymentProp.currency = currency;
            paymentProp.bank = bank;
            paymentProp.paymentType = paymentType;
            paymentProp.collectedBy = collectedBy;
            paymentProp.chequeNo = chequeNo;
            paymentProp.bank = bank;
            paymentProp.date = new Date();
            paymentProp.transactionId = "MANUAL";
            paymentProp.tags = tags;

            PaymentProp prop = Payment.recordPayment(client, paymentProp, null, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "query", httpMethod = HttpMethod.POST)
    public APIResponse query(@Named("client") ClientEnum clientEnum,
                             @Nullable @Named("currency") Currency currency,
                             @Nullable @Named("paymentType") PaymentType paymentType,
                             @Nullable @Named("collectedByEmail") String collectedBy,
                             @Nullable @Named("chequeNo") String chequeNo,
                             @Nullable @Named("dateRange") DateRange dateRange, @Nullable @Named("tags") Set<String> tags,
                             @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client =
                Utils.removeSpaceUnderscoreBracketAndHyphen(clientEnum.toString().toLowerCase());

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);

            PaymentQueryCondition qc = new PaymentQueryCondition();
            qc.chequeNo = chequeNo;
            qc.collectedBy = collectedBy;
            qc.currency = currency;
            qc.paymentType = paymentType;

            if (dateRange != null) {
                qc.endDate = new Date();
                long startDateMS = qc.endDate.getTime() - DateUtils.getMilliSecondsFromDateRange(dateRange);
                qc.startDate = new Date(startDateMS);
            }

            qc.tags = tags;

            List<PaymentEntity> entities = Payment.query(client, qc, login);

            List<PaymentProp> props = new ArrayList<>();

            for (PaymentEntity entity : entities) {
                props.add(entity.toProp());
            }

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
