package crmdna.common.contact;

import crmdna.common.Utils;
import crmdna.common.contact.Contact.Gender;

import java.util.HashSet;
import java.util.Set;

public class ContactProp implements Comparable<ContactProp> {
    public String email;
    public String firstName;
    public String lastName;
    public String nickName;

    public String occupation;
    public String company;
    public PostalAddressProp homeAddress = new PostalAddressProp();
    public PostalAddressProp officeAddress = new PostalAddressProp();

    public String homePhone;
    public String officePhone;
    public String mobilePhone;

    public Gender gender;

    public int asOfyyyymmdd;

    public String getName() {
        return Utils.getFullName(firstName, lastName);
    }

    public String getPhoneNos() {
        // TODO add test case

        String phoneNos = "";
        if (mobilePhone != null)
            phoneNos += mobilePhone + " ";

        if (officePhone != null)
            phoneNos += officePhone + " ";

        if (homePhone != null)
            phoneNos += homePhone;

        return phoneNos;
    }

    public Set<String> getEmailAndPhoneNosAsSet() {
        Set<String> set = new HashSet<>();

        if (email != null)
            set.add(email.toLowerCase());

        if (mobilePhone != null)
            set.add(mobilePhone);

        if (officePhone != null)
            set.add(officePhone);

        if (homePhone != null)
            set.add(homePhone);

        return set;
    }

    @Override
    public int compareTo(ContactProp o) {
        if ((firstName != null) && (o.firstName != null))
            return firstName.compareTo(o.firstName);

        if ((email != null) && (o.email != null))
            return email.compareTo(o.email);

        return 0;
    }
}
