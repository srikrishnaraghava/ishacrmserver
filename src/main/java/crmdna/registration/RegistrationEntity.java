package crmdna.registration;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.common.Utils.PaypalErrorType;
import crmdna.common.contact.Contact.Gender;
import crmdna.registration.Registration.RegistrationStatus;

import java.util.*;

@Entity
@Cache
public class RegistrationEntity {
    @Id
    long registrationId;
    String firstName;
    String lastName;
    String nickName;
    String email;
    Gender gender;
    String mobilePhone;
    String homePhone;
    String officePhone;
    String homeAddress;
    String officeAddress;

    @Index
    long memberId;
    @Index
    long programId;

    String paymentUrl;
    String redirectUrl;

    String marketingChannel;

    String successCallbackUrl;
    String errorCallbackUrl;

    // pay pal fields
    String amount;
    String ccy;
    boolean isPaymentPending;
    String pendingReason;
    PaypalErrorType paypalErrorType;
    String L_SEVERITYCODE0;
    String L_ERRORCODE0;
    String L_SHORTMESSAGE0;
    String L_LONGMESSAGE0;

    @Index
    String transactionId;
    @Index
    Set<String> qsTags = new HashSet<>();
    @Index
    private RegistrationStatus status;
    // track different states
    private List<Date> stateChangeTimestamps = new ArrayList<>();
    private List<RegistrationStatus> statuses = new ArrayList<>();

    RegistrationStatus getStatus() {
        return status;
    }

    Date getStatusTimestamp() {
        if (stateChangeTimestamps.size() == 0)
            return null;

        int size = stateChangeTimestamps.size();
        return stateChangeTimestamps.get(size - 1);
    }

    void recordStateChange(RegistrationStatus rs) {
        status = rs;
        stateChangeTimestamps.add(new Date());
        statuses.add(rs);
    }

    public RegistrationProp toProp() {
        RegistrationProp prop = new RegistrationProp();

        prop.registrationId = registrationId;
        prop.firstName = firstName;
        prop.lastName = lastName;
        prop.nickName = nickName;
        prop.email = email;
        prop.mobilePhone = mobilePhone;
        prop.homePhone = homePhone;
        prop.officePhone = officePhone;
        prop.homeAddress = homeAddress;
        prop.officeAddress = officeAddress;
        prop.memberId = memberId;
        prop.programId = programId;

        prop.paymentUrl = paymentUrl;
        prop.redirectUrl = redirectUrl;

        prop.successCallbackUrl = successCallbackUrl;
        prop.errorCallbackUrl = errorCallbackUrl;

        prop.amount = amount;
        prop.ccy = ccy;
        prop.transactionId = transactionId;
        prop.isPaymentPending = isPaymentPending;
        prop.pendingReason = pendingReason;
        prop.paypalErrorType = paypalErrorType;
        prop.L_SEVERITYCODE0 = L_SEVERITYCODE0;
        prop.L_ERRORCODE0 = L_ERRORCODE0;
        prop.L_SHORTMESSAGE0 = L_SHORTMESSAGE0;
        prop.L_LONGMESSAGE0 = L_LONGMESSAGE0;

        for (int i = 0; i < statuses.size(); i++) {
            RegistrationStatusChange rsc = new RegistrationStatusChange();
            rsc.newStatus = statuses.get(i);
            rsc.timestamp = stateChangeTimestamps.get(i);

            prop.changes.add(rsc);
        }

        prop.status = status;

        return prop;
    }
}
