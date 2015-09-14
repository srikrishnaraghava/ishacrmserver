package crmdna.payment2;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;


public class Payment {
    public static PaymentProp recordPayment(String client, PaymentProp paymentProp, Set<String> tags,
                                            String login) {
        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        ensureNotNull(paymentProp, "paymentProp is null");

        ensure(paymentProp.amount != 0, "Amount is 0");
        ensureNotNull(paymentProp.currency, "Currency is null");
        ensureNotNull(paymentProp.transactionId, "Transaction id is null");
        ensureNotNull(paymentProp.paymentType, "Payment type is null");

        if ((paymentProp.paymentType == PaymentType.CASH)
                || (paymentProp.paymentType == PaymentType.CHEQUE)) {
            ensureNotNull(paymentProp.collectedBy, "Collected by is null");
            Utils.ensureValidEmail(paymentProp.collectedBy);
        }

        if (paymentProp.paymentType == PaymentType.CHEQUE) {
            ensureNotNull(paymentProp.chequeNo, "Cheque no should be specified");
            ensureNotNull(paymentProp.bank, "Bank should be specified");
        }

        // TODO: cash and online cannot have chequeNo and bank

        if (!paymentProp.collectedBy.equals(login)) {
            User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PAYMENT);
        }

        PaymentEntity entity = new PaymentEntity();
        entity.paymentId = Sequence.getNext(client, SequenceType.PAYMENT);
        entity.amount = paymentProp.amount;
        entity.currency = paymentProp.currency;
        entity.paymentType = paymentProp.paymentType;
        entity.bank = paymentProp.bank;
        entity.chequeNo = paymentProp.chequeNo;
        entity.collectedBy = paymentProp.collectedBy;
        entity.currency = paymentProp.currency;
        entity.ms = paymentProp.date.getTime();
        entity.tags = tags;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static List<PaymentEntity> query(String client, PaymentQueryCondition qc, String login) {
        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        ensureNotNull(qc, "query condition is null");

        Query<PaymentEntity> q = ofy(client).load().type(PaymentEntity.class);
        if (qc.currency != null)
            q = q.filter("currency", qc.currency);

        if (qc.transactionId != null)
            q = q.filter("transactionId", qc.transactionId);

        if (qc.paymentType != null)
            q = q.filter("paymentType", qc.paymentType);

        if (qc.chequeNo != null)
            q = q.filter("chequeNo", qc.chequeNo);

        if (qc.collectedBy != null)
            q = q.filter("collectedBy", qc.collectedBy);

        if (qc.startDate != null)
            q = q.filter("ms >", qc.startDate.getTime());

        if (qc.endDate != null)
            q = q.filter("ms <", qc.endDate.getTime());

        if ((qc.tags != null) && !qc.tags.isEmpty()) {
            for (String tag : qc.tags) {
                q = q.filter("tags", tag);
            }
        }

        q = q.order("-ms");

        List<Key<PaymentEntity>> keys = q.keys().list();

        final int MAX_ENTITIES = 5000;
        if (keys.size() > MAX_ENTITIES)
            throw new APIException("Query condition returns more than 5000 entities")
                    .status(Status.ERROR_OVERFLOW);

        Map<Key<PaymentEntity>, PaymentEntity> map = ofy(client).load().keys(keys);

        List<PaymentEntity> entities = new ArrayList<>();
        for (Key<PaymentEntity> key : keys) {
            entities.add(map.get(key));
        }

        return entities;
    }

    public enum PaymentType {
        CASH, CHEQUE, ONLINE
    }
}
