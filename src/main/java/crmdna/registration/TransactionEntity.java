package crmdna.registration;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Cache
public class TransactionEntity {
    @Id
    String transactionId;
    long registrationId;
}
