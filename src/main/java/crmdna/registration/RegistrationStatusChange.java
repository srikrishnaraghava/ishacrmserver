package crmdna.registration;

import crmdna.registration.Registration.RegistrationStatus;

import java.util.Date;

public class RegistrationStatusChange {
    public Date timestamp;
    public RegistrationStatus newStatus;
    public String userFriendlyMessage;
}
