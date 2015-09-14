package crmdna.mail2;

import crmdna.client.Client;
import crmdna.group.Group;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.user.User;

import java.util.Map;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNullNotEmpty;

/**
 * Created by sathya on 11/9/15.
 */
public class MailMapFactory {

    public static MailMap getFromMemberQueryCondition(MemberQueryCondition mqc, long groupId, String defaultFirstName,
                                                      String defaultLastName, String login) {

        Client.ensureValid(mqc.client);
        Group.safeGet(mqc.client, groupId);

        User.ensureGroupLevelPrivilege(mqc.client, groupId, login, User.GroupLevelPrivilege.SEND_EMAIL);

        mqc.subscribedGroupIds.add(groupId);

        java.util.List<MemberProp> memberProps = MemberLoader.queryProps(mqc, login);

        MailMap mailMap = new MailMap();
        for (MemberProp memberProp : memberProps) {

            String email = memberProp.contact.email;
            if (email == null)
                continue;

            String firstName = memberProp.contact.firstName;
            if ((firstName == null) || firstName.isEmpty()) {
                ensureNotNullNotEmpty(defaultFirstName, "Default firstName not specified");
                firstName = defaultFirstName;
            }

            mailMap.add(email, MailMap.MergeVarID.FIRST_NAME, firstName);

            String lastName = memberProp.contact.lastName;
            if ((lastName == null) || lastName.isEmpty()) {
                ensureNotNullNotEmpty(defaultLastName, "Default lastName not specified");
                lastName = defaultLastName;
            }

            mailMap.add(email, MailMap.MergeVarID.LAST_NAME, lastName);
        }

        return mailMap;
    }

    public static MailMap getFromListOfMap(java.util.List<Map<String, String>> listOfMap,
                                           String defaultFirstName, String defaultLastName) {

        MailMap mailMap = new MailMap();
        for (int i = 0; i < listOfMap.size(); i++) {
            Map<String, String> map = listOfMap.get(i);

            ensure(map.containsKey("firstname"));
            ensure(map.containsKey("lastname"));
            ensure(map.containsKey("email"));

            String firstName = map.get("firstname");
            if (firstName == null) {
                firstName = "";
            }
            firstName = firstName.trim();

            String lastName = map.get("lastname");
            if (lastName == null) {
                lastName = "";
            }
            lastName = lastName.trim();

            if (firstName.isEmpty())
                firstName = defaultFirstName;

            if (lastName.isEmpty() && !firstName.equals(defaultFirstName)) {
                String[] split = firstName.split("\\s+");
                if (split.length > 1 && !split[0].contains(".")) {
                    firstName = split[0];

                    //rest is last name
                    lastName = split[1];
                    for (int j = 2; j < split.length; j++) {
                        lastName = lastName + " " + split[j];
                    }
                }
            }

            if (lastName.isEmpty()) {
                lastName = defaultLastName;
            }

            String email = map.get("email").trim();

            mailMap.add(email, firstName, lastName);
        }

        return mailMap;
    }
}
