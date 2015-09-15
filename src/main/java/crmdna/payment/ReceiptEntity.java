package crmdna.payment;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.condition.IfNotNull;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.registration.Registration;
import crmdna.registration.RegistrationProp;
import crmdna.payment.Receipt.Purpose;
import crmdna.sequence.Sequence;

import java.util.Date;

@Entity
@Cache
public class ReceiptEntity {

    @Id
    String id;

    @Index(IfNotNull.class)
    Long registrationId;

    @Index
    long groupId;

    @Index
    long ms;

    @Index(IfNotNull.class)
    String firstName;

    @Index(IfNotNull.class)
    String lastName;

    @Index(IfNotNull.class)
    String email;

    @Index(IfNotNull.class)
    Purpose purpose;

    @Index(IfNotNull.class)
    Long programId; // Store the reference when the purpose is VOLUNTEER_CONTRIBUTION

    @Index(IfNotNull.class)
    String adhocReference; // Store the reference when the purpose is ADHOC_DONATION

    Currency ccy;

    @Index(IfNotNull.class)
    Double amount;

    ReceiptEntity(String client, long groupId) {
        this.id = Receipt.getReceiptId(groupId,
            Sequence.getNextAtGroupLevel(client, groupId, Sequence.GroupLevelSequenceType.RECEIPT));
        this.groupId = groupId;
        this.ms = (new Date()).getTime();
    }

    public ReceiptProp toProp(String client) {
        ReceiptProp receiptProp = new ReceiptProp();
        receiptProp.receiptId = id;
        receiptProp.groupId = groupId;
        receiptProp.ms = ms;
        receiptProp.registrationId = registrationId;
        if (purpose == Purpose.PROGRAM_REGISTRATION) {
            RegistrationProp registrationProp = Registration.safeGet(client, registrationId).toProp();
            ProgramProp programProp = Program.get(client, registrationProp.programId).toProp(client);

            receiptProp.firstName = registrationProp.firstName;
            receiptProp.lastName = registrationProp.lastName;
            receiptProp.email = registrationProp.email;
            receiptProp.purpose = programProp.getDetailedName();
            receiptProp.ccy = Utils.Currency.valueOf(registrationProp.ccy);
            receiptProp.amount = Double.parseDouble(registrationProp.amount);
        } else {
            receiptProp.firstName = firstName;
            receiptProp.lastName =  lastName;
            receiptProp.email = email;
            receiptProp.ccy = ccy;
            receiptProp.amount = amount;

            if (purpose == Purpose.VOLUNTEER_CONTRIBUTION) {
                ProgramProp programProp = Program.get(client, programId).toProp(client);
                receiptProp.purpose = purpose.toString() + " - " + programProp.getDetailedName();
            } else if (purpose == Purpose.ADHOC_DONATION) {
                receiptProp.purpose = adhocReference;
            }
        }

        return receiptProp;
    }
}
