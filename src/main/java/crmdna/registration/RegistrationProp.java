package crmdna.registration;

import crmdna.common.Utils;
import crmdna.common.Utils.PaypalErrorType;
import crmdna.registration.Registration.RegistrationStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RegistrationProp {

    public long registrationId;
    public String firstName;
    public String lastName;
    public String nickName;
    public String email;
    public String mobilePhone;
    public String homePhone;
    public String officePhone;
    public String homeAddress;
    public String officeAddress;

    public long memberId;
    public long programId;

    public String paymentUrl;
    public String redirectUrl;

    public String successCallbackUrl;
    public String errorCallbackUrl;

    public String amount;
    public String ccy;
    public String transactionId;

    public boolean isPaymentPending;
    public String pendingReason;
    public PaypalErrorType paypalErrorType;
    public String L_SEVERITYCODE0;
    public String L_ERRORCODE0;
    public String L_SHORTMESSAGE0;
    public String L_LONGMESSAGE0;

    public List<RegistrationStatusChange> changes = new ArrayList<>();
    public RegistrationStatus status;

    public Boolean alreadyRegistered; // this field is not in registration
    // entity

    public RegistrationStatus getStatus() {
        if (changes.size() == 0)
            return null;

        return changes.get(changes.size() - 1).newStatus;
    }

    public Date getStatusTimestamp() {
        if (changes.size() == 0)
            return null;

        return changes.get(changes.size() - 1).timestamp;
    }

    public String getName() {
        return Utils.getFullName(firstName, lastName);
    }
}
