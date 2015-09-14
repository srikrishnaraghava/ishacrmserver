package crmdna.participant;

import crmdna.common.contact.ContactProp;

public class ParticipantProp implements Comparable<ParticipantProp> {
    public long participantId;
    public ContactProp contactDetail = new ContactProp();
    ;

    public long programId;
    public long memberId;

    @Override
    public int compareTo(ParticipantProp arg0) {

        if ((contactDetail == null) || (contactDetail.firstName == null))
            return 1;

        if ((arg0 == null) || (arg0.contactDetail == null) || (arg0.contactDetail.firstName == null))
            return -1;

        return contactDetail.firstName.toLowerCase()
                .compareTo(arg0.contactDetail.firstName.toLowerCase());
    }
}
