package crmdna.member;

import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfTrue;
import crmdna.client.Client;
import crmdna.common.contact.Contact.Gender;
import crmdna.common.contact.ContactProp;
import crmdna.member.Member.AccountType;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;

import java.util.*;

import static crmdna.common.AssertUtils.*;

@Entity
@Cache
public class MemberEntity {

    @Id
    long memberId;
    @Index
    String email;
    String firstName;
    String lastName;
    Gender gender;
    String homeAddress;
    String homeCity;
    String homeState;
    String homeCountry;
    String homePincode;
    String occupation;
    String company;
    String officeAddress;
    String officePincode;
    String homePhone;
    String officePhone;
    @Index
    String mobilePhone;
    @Index
    Set<Long> groupIds = new HashSet<>();
    @Index
    Set<String> qsTags;
    @Index
    Set<Long> programIds = new HashSet<>();
    @Serialize(zip = true)
    TreeMap<Integer, UnverifiedProgramProp> uvpMap = new TreeMap<>();
    Map<String, String> customFields;
    int asOfYYYYMMDD; // contact details valid as of this date
    // dependents
    @Index
    String name;
    @Index
    String nameFirstChar;
    @Index
    String firstName3Char;
    @Index
    Set<Long> programTypeIds = new HashSet<>();
    @Index
    Set<Long> practiceIds = new HashSet<>();
    @Index(IfTrue.class)
    boolean hasAccount;
    AccountType accountType;
    byte[] encryptedPwd;
    byte[] salt;
    @Index(IfTrue.class)
    boolean isEmailVerified;
    int verificationCode;
    boolean accountDisabled;
    long accountCreatedMS;
    @Index
    Set<Long> subscribedListIds = new HashSet<>();
    @Index
    Set<Long> unsubscribedListIds = new HashSet<>();
    @Index
    Set<Long> listIds = new HashSet<>();

    @Index
    Set<Long> subscribedGroupIds = new HashSet<>();
    @Index
    Set<Long> unsubscribedGroupIds = new HashSet<>();

    public MemberProp toProp() {
        MemberProp prop = new MemberProp();
        prop.memberId = memberId;
        prop.contact = new ContactProp();
        prop.contact.email = email;
        prop.contact.firstName = firstName;
        prop.contact.lastName = lastName;
        prop.contact.gender = gender;
        prop.contact.homeAddress.address = homeAddress;
        prop.contact.homeAddress.city = homeCity;
        prop.contact.homeAddress.country = homeCountry;
        prop.contact.homeAddress.pincode = homePincode;

        prop.contact.occupation = occupation;
        prop.contact.company = company;
        prop.contact.officeAddress.address = officeAddress;
        prop.contact.officeAddress.pincode = officePincode;

        prop.contact.homePhone = homePhone;
        prop.contact.officePhone = officePhone;
        prop.contact.mobilePhone = mobilePhone;

        prop.contact.asOfyyyymmdd = asOfYYYYMMDD;

        prop.name = name;
        // prop.qsTags = qsTags;
        prop.groupIds = groupIds;

        prop.programIds = programIds;
        prop.programTypeIds = programTypeIds;
        prop.practiceIds = practiceIds;

        prop.hasAccount = hasAccount;
        prop.accountType = accountType;
        prop.isEmailVerified = isEmailVerified;
        prop.accountDisabled = accountDisabled;
        prop.salt = salt;
        prop.encryptedPwd = encryptedPwd;
        prop.accountCreatedMS = accountCreatedMS;
        prop.verificationCode = verificationCode;

        prop.subscribedListIds = subscribedListIds;
        prop.unsubscribedListIds = unsubscribedListIds;
        prop.listIds = listIds;
        prop.subscribedGroupIds = subscribedGroupIds;
        prop.unsubscribedGroupIds = unsubscribedGroupIds;

        return prop;
    }

    public long getId() {
        return memberId;
    }

    public boolean isSelf(String login) {
        ensureNotNull(login, "login is null");

        if (email != null && email.equalsIgnoreCase(login))
            return true;

        return false;
    }

    public static class MemberFactory {

        public static List<MemberEntity> create(String client, int howMany) {

            final int MAX_MEMBERS = 10000; // just a safety limit, can be
            // increased if required

            Client.ensureValid(client);

            ensure(howMany <= MAX_MEMBERS, "contacts size should be less than or equal to ["
                    + MAX_MEMBERS + "]");

            List<MemberEntity> memberEntities = new ArrayList<>(howMany);
            List<Long> memberIds = Sequence.getNext(client, SequenceType.MEMBER, howMany);

            ensureEqual(howMany, memberIds.size());

            for (int i = 0; i < howMany; i++) {
                MemberEntity memberEntity = new MemberEntity();
                memberEntity.memberId = memberIds.get(i);
                memberEntities.add(memberEntity);
            }

            return memberEntities;
        }
    }
}
