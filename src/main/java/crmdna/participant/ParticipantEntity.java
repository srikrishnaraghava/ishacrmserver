package crmdna.participant;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.client.Client;
import crmdna.common.contact.Contact.Gender;
import crmdna.common.contact.ContactProp;
import crmdna.member.MemberEntity;
import crmdna.program.Program;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.AssertUtils.ensureEqual;
import static crmdna.common.AssertUtils.ensureNotNull;

@Entity
@Cache
public class ParticipantEntity {

    @Id
    long participantId;
    @Index
    String email;
    String firstName;
    String lastName;
    Gender gender;
    String homePhone;
    String officePhone;
    @Index
    String mobilePhone;
    String homeAddress;
    String homeCity;
    String homeState;
    String homeCountry;
    String homePincode;
    String occupation;
    String company;
    String officeAddress;
    String officePincode;
    @Index
    long programId;
    @Index
    long memberId;

    public ParticipantProp toProp() {
        ParticipantProp prop = new ParticipantProp();
        prop.participantId = participantId;

        prop.contactDetail.email = email;
        prop.contactDetail.firstName = firstName;
        prop.contactDetail.lastName = lastName;
        prop.contactDetail.gender = gender;
        prop.contactDetail.homePhone = homePhone;
        prop.contactDetail.officePhone = officePhone;
        prop.contactDetail.mobilePhone = mobilePhone;

        prop.contactDetail.homeAddress.address = homeAddress;
        prop.contactDetail.homeAddress.city = homeCity;
        prop.contactDetail.homeAddress.state = homeState;
        prop.contactDetail.homeAddress.country = homeCountry;

        prop.contactDetail.officeAddress.address = officeAddress;
        prop.contactDetail.occupation = occupation;
        prop.contactDetail.company = company;

        // copy city, state and country from home
        prop.contactDetail.officeAddress.city = homeCity;
        prop.contactDetail.officeAddress.state = homeState;
        prop.contactDetail.officeAddress.country = homeCountry;
        prop.contactDetail.officeAddress.pincode = officePincode;

        prop.programId = programId;
        prop.memberId = memberId;
        prop.programId = programId;

        return prop;
    }

    static class ParticipantFactory {
        static List<ParticipantEntity> create(String client,
                                              List<ContactProp> contacts, List<MemberEntity> memberEntities, long programId) {

            Client.ensureValid(client);
            Program.safeGet(client, programId);

            ensureNotNull(contacts);
            ensureEqual(contacts.size(), memberEntities.size(), "No of contacts ["
                    + contacts.size() + "] is different from no of members ["
                    + memberEntities.size() + "]");

            // none of the contact or member entity can be null
            for (int i = 0; i < contacts.size(); i++) {
                ensureNotNull(contacts.get(i));
                ensureNotNull(memberEntities.get(i));
            }

            List<ParticipantEntity> entities = new ArrayList<>(contacts.size());

            List<Long> ids = Sequence.getNext(client, SequenceType.PARTICIPANT,
                    contacts.size());

            for (int i = 0; i < contacts.size(); i++) {
                ContactProp c = contacts.get(i);
                ParticipantEntity pe = new ParticipantEntity();
                pe.participantId = ids.get(i);

                pe.email = c.email;
                pe.firstName = c.firstName;
                pe.lastName = c.lastName;
                pe.gender = c.gender;
                // pe.groupId = c.centerId;

                pe.homeAddress = c.homeAddress.address;

                pe.homePhone = c.homePhone;
                pe.mobilePhone = c.mobilePhone;
                pe.lastName = c.lastName;
                pe.occupation = c.occupation;
                pe.company = c.company;
                pe.officeAddress = c.officeAddress.address;
                pe.officePhone = c.officePhone;
                pe.programId = programId;

                pe.memberId = memberEntities.get(i).getId();

                entities.add(pe);
            }

            return entities;
        }
    }
}
