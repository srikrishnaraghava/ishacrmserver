package crmdna.registration;

import crmdna.registration.Registration.RegistrationStatus;

import java.util.Date;

public class RegistrationStatusProp {

    public boolean alreadyRegistered;
    public boolean hasAttemptedRegistrationBefore;

    public RegistrationStatus lastRegistrationStatus;
    public Date registrationStatusTimestamp;
}
