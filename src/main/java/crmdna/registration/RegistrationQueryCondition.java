package crmdna.registration;

import crmdna.registration.Registration.RegistrationStatus;

public class RegistrationQueryCondition {
    public Long programId;
    public String searchStr;
    public RegistrationStatus status;

    public boolean sortByFirstName = true;
}
