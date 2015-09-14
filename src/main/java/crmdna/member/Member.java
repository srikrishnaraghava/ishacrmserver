package crmdna.member;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.DateUtils.Month;
import crmdna.common.MemcacheLock;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.Contact;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.list.ListEntity;
import crmdna.list.ListProp;
import crmdna.mail2.MailMap;
import crmdna.member.MemberEntity.MemberFactory;
import crmdna.program.Program;
import crmdna.program.ProgramEntity;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.User.ResourceType;
import crmdna.user.UserCore;

import java.util.*;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;
import static crmdna.member.MemberBulkSaver.memberBulkSaver;

public class Member {

    public static MemberProp create(String client, long groupId, ContactProp contact,
                                    boolean allowDuplicateEmail, String login) {

        Client.ensureValid(client);

        User.ensureValidUser(client, login);
        Group.safeGet(client, groupId);

        ensureNotNull(contact, "contact is null");

        Contact.ensureEmailOrPhoneNumberValid(contact);

        if (null != contact.email)
            contact.email = contact.email.toLowerCase();

        if (!allowDuplicateEmail) {
            // if email is specified - it should be unique
            if (null != contact.email) {
                ensureEmailNotPresentInDB(client, contact.email, login);
            }
        }

        try (MemcacheLock lock = new MemcacheLock(client, ResourceType.MEMBER, contact.email)) {

            MemberEntity memberEntity = MemberFactory.create(client, 1).get(0);

            memberBulkSaver(client, Utils.getList(memberEntity))
                    .setContactsSameSizeList(Utils.getList(contact)).addGroupToAll(groupId)
                    .populateDependantsAndSave();
            ObjectifyFilter.complete();

            return memberEntity.toProp();
        }
    }

    static void ensureEmailNotPresentInDB(String client, String email, String login) {

        ensureNotNull(email);
        email = email.toLowerCase();

        MemberQueryCondition condition = new MemberQueryCondition(client, 10000);
        condition.email = email;

        List<Key<MemberEntity>> keys = MemberLoader.queryKeys(condition, login);

        if (keys.size() != 0)
            throw new APIException("There is already a member with email [" + email + "]")
                    .status(Status.ERROR_RESOURCE_ALREADY_EXISTS);
    }

    public static MemberProp updateContactDetails(final String client, final long memberId,
                                                  final ContactProp contact, String login) {

        Client.ensureValid(client);
        ensureNotNull(contact, "contact is null");
        ensureNotNull(login, "login is null");

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);

        // member can update his own record, otherwise will need to be valid user
        if ((memberEntity.email == null) || !memberEntity.email.equalsIgnoreCase(login))
            User.ensureValidUser(client, login);

        // if email is different it should not exist in a different entity
        if ((memberEntity.email != null) && (contact.email != null)
                && (!memberEntity.email.toLowerCase().equals(contact.email.toLowerCase()))) {

            contact.email = contact.email.toLowerCase();
            ensureEmailNotPresentInDB(client, contact.email, login);
        }

        try (MemcacheLock lock = new MemcacheLock(client, ResourceType.MEMBER, contact.email)) {

            memberBulkSaver(client, Utils.getList(memberEntity)).setContactsSameSizeList(
                    Utils.getList(contact)).populateDependantsAndSave();
        }

        return memberEntity.toProp();
    }

    public static MemberProp addOrDeleteGroup(String client, long memberId, long groupId,
                                              boolean add, String login) {

        Client.ensureValid(client);
        Group.safeGet(client, groupId);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);

        boolean change;
        if (add)
            change = memberEntity.groupIds.add(groupId);
        else {
            change = memberEntity.groupIds.remove(groupId);

            if (memberEntity.groupIds.isEmpty())
                throw new APIException("The only group [" + groupId + "] cannot be removed from member ["
                        + memberId + "]").status(Status.ERROR_PRECONDITION_FAILED);
        }

        if (change)
            ofy(client).save().entity(memberEntity).now();

        return memberEntity.toProp();
    }

    public static MemberProp addOrDeleteProgram(String client, long memberId, long programId,
                                                boolean add, String login) {
        Client.ensureValid(client);
        UserCore.ensureValidUser(client, login);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        Program.safeGet(client, programId);

        if (add) {
            memberBulkSaver(client, Utils.getList(memberEntity)).addProgramToAll(programId)
                    .populateDependantsAndSave();
        } else {
            memberBulkSaver(client, Utils.getList(memberEntity)).deleteProgramFromAll(programId)
                    .populateDependantsAndSave();
        }

        return memberEntity.toProp();
    }

    //returns true if member entity changed
    public static boolean addOrDeleteList(String client, long memberId, long listId,
                                              boolean add, String login) {

        Client.ensureValid(client);
        ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);

        if (!listProp.enabled)
            throw new APIException("List [" + listId + "] is disabled")
                    .status(Status.ERROR_PRECONDITION_FAILED);

        //need privilege if not self or list is restricted
        if (!memberEntity.isSelf(login) || listProp.restricted) {
            User.ensureGroupLevelPrivilege(client, listProp.groupId, login,
                    GroupLevelPrivilege.UPDATE_LIST);
        }

        boolean change;
        if (add)
            change = memberEntity.listIds.add(listId);
        else {
            change = memberEntity.listIds.remove(listId);
        }

        populateDependantFields(client, Utils.getList(memberEntity));

        if (change)
            ofy(client).save().entity(memberEntity).now();

        return change;
    }

    // returns true if member list changed as a result of this call, else false
    public static boolean subscribeList_to_be_removed(String client, long memberId, long listId, String login) {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

        if (!listProp.enabled)
            throw new APIException("Cannot (un)subscribe for disabled list [" + listId + "]")
                    .status(Status.ERROR_PRECONDITION_FAILED);

        //need privilege if not self or list is restricted
        if (!memberEntity.isSelf(login) || listProp.restricted) {
            User.ensureGroupLevelPrivilege(client, listProp.groupId, login,
                    GroupLevelPrivilege.UPDATE_LIST);
        }

        if (memberEntity.subscribedListIds.contains(listId))
            return false;

        memberEntity.subscribedListIds.add(listId);

        populateDependantFields(client, Utils.getList(memberEntity));
        ofy(client).save().entity(memberEntity).now();

        return true;
    }

    // returns true if member list changed as a result of this call, else false
    public static boolean unsubscribeList_to_be_removed(String client, long memberId, long listId, String login) {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

        if (!memberEntity.isSelf(login))
            User.ensureGroupLevelPrivilege(client, listProp.groupId, login,
                    GroupLevelPrivilege.UPDATE_LIST);

        if (!memberEntity.unsubscribedListIds.contains(listId))
            return false;

        memberEntity.unsubscribedListIds.remove(listId);

        populateDependantFields(client, Utils.getList(memberEntity));
        ofy(client).save().entity(memberEntity).now();

        return true;
    }

    // returns true if member list changed as a result of this call, else false
    public static boolean subscribeGroup(String client, long memberId, long groupId, String login) {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        Group.safeGet(client, groupId);

        if (!memberEntity.isSelf(login)) {
            User.ensureValidUser(client, login);

            if (memberEntity.unsubscribedGroupIds.contains(groupId)){
                User.ensureClientLevelPrivilege(client, login, User.ClientLevelPrivilege.SUBSCRIBE_GROUP);
            }
        }

        if (memberEntity.subscribedGroupIds.contains(groupId)) {
            return false;
        }

        memberEntity.subscribedGroupIds.add(groupId);
        memberEntity.unsubscribedGroupIds.remove(groupId);

        ofy(client).save().entity(memberEntity).now();

        return true;
    }

    // returns true if member list changed as a result of this call, else false
    public static boolean unsubscribeGroup(String client, long memberId, long groupId, String login) {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        Group.safeGet(client, groupId);

        if (!memberEntity.isSelf(login)) {
            User.ensureValidUser(client, login);
        }

        if (memberEntity.unsubscribedGroupIds.contains(groupId)) {
            return false;
        }

        memberEntity.subscribedGroupIds.remove(groupId);
        memberEntity.unsubscribedGroupIds.add(groupId);

        ofy(client).save().entity(memberEntity).now();

        return true;
    }

    public static Long getMatchingMemberId(String client, String email, String firstName,
                                           String lastName) {
        List<ContactProp> contactDetailProps = new ArrayList<>();

        ContactProp c = new ContactProp();
        c.firstName = firstName;
        c.lastName = lastName;
        c.email = email;

        contactDetailProps.add(c);

        return getMatchingMemberIds(client, contactDetailProps).get(0);
    }

    public static Map<String, Long> getMemberIdFromEmailIfExistsElseCreateAndGet(String client,
                                                                                 MailMap mailMap, long groupId) {

        Client.ensureValid(client);
        Group.safeGet(client, groupId);

        ensureNotNull(mailMap, "mailMap is null");

        Set<String> emails = mailMap.getEmails();
        if (emails.isEmpty())
            return new HashMap<>();

        List<Key<MemberEntity>> memberKeys =
                ofy(client).load().type(MemberEntity.class).filter("email in", emails).keys().list();

        List<MemberEntity> memberEntities = new ArrayList<>();
        if (!memberKeys.isEmpty())
            memberEntities =
                    ofy(client).load().type(MemberEntity.class).filterKey("in", memberKeys).project("email")
                            .list();

        Map<String, Long> emailVsMemberId = new HashMap<>();
        Set<String> missing = new HashSet<>(emails);

        for (MemberEntity memberEntity : memberEntities) {
            if (memberEntity.email == null)
                continue;

            if (emails.contains(memberEntity.email)) {
                missing.remove(memberEntity.email);
                emailVsMemberId.put(memberEntity.email, memberEntity.memberId);
            }
        }

        if (missing.isEmpty())
            return emailVsMemberId;

        // create member entities for missing emails
        ensure(!missing.isEmpty());

        List<MemberEntity> newMemberEntities = MemberFactory.create(client, missing.size());
        ensureEqual(missing.size(), newMemberEntities.size(), "newMemberEntities size mismatch");

        List<ContactProp> contacts = new ArrayList<>();
        for (String email : emails) {

            if (!missing.contains(email))
                continue;

            ContactProp contact = new ContactProp();
            contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
            contact.firstName = mailMap.get(MailMap.MergeVarID.FIRST_NAME, email);
            contact.lastName = mailMap.get(MailMap.MergeVarID.LAST_NAME, email);
            contact.email = email;

            contacts.add(contact);
        }

        memberBulkSaver(client, newMemberEntities).setContactsSameSizeList(contacts)
                .addGroupToAll(groupId).populateDependantsAndSave();

        for (MemberEntity memberEntity : newMemberEntities) {
            emailVsMemberId.put(memberEntity.email, memberEntity.memberId);
        }

        return emailVsMemberId;
    }

    public static Map<String, Long> getMemberIdFromEmail(String client, Set<String> emails) {

        Client.ensureValid(client);
        ensureNoNullElement(emails);

        if (emails.isEmpty())
            return new HashMap<>();

        Map<String, List<Key<MemberEntity>>> emailVsKeys = new HashMap<>();
        for (String email : emails) {
            List<Key<MemberEntity>> keys =
                    ofy(client).load().type(MemberEntity.class).filter("email", email).keys().list();

            emailVsKeys.put(email, keys);
        }

        Map<String, Long> emailVsMemberId = new HashMap<>();

        for (String email : emailVsKeys.keySet()) {
            List<Key<MemberEntity>> keys = emailVsKeys.get(email);

            if (!keys.isEmpty()) {
                long memberId = keys.get(0).getId();
                emailVsMemberId.put(email, memberId);
            }

        }

        return emailVsMemberId;
    }

    public static List<Long> getMatchingMemberIds(String client, List<ContactProp> contactDetailProps) {

        Client.ensureValid(client);

        if (contactDetailProps.size() == 0)
            return new ArrayList<>();

        for (ContactProp contactDetailProp : contactDetailProps) {
            if (contactDetailProp.email != null)
                contactDetailProp.email = contactDetailProp.email.toLowerCase();
        }

        Map<String, Set<Integer>> emailMap = new HashMap<>();
        Map<String, Set<Integer>> mobileMap = new HashMap<>();

        for (int index = 0; index < contactDetailProps.size(); index++) {
            ContactProp c = contactDetailProps.get(index);
            if (c.email != null) {
                if (!emailMap.containsKey(c.email))
                    emailMap.put(c.email, new HashSet<Integer>());

                Set<Integer> existing = emailMap.get(c.email);
                existing.add(index);
            } else if (c.mobilePhone != null) {
                if (!mobileMap.containsKey(c.mobilePhone))
                    mobileMap.put(c.mobilePhone, new HashSet<Integer>());

                Set<Integer> existing = mobileMap.get(c.mobilePhone);
                existing.add(index);
            }
        }

        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < contactDetailProps.size(); i++) {
            memberIds.add(null);
        }

        List<MemberEntity> emailMatches = new ArrayList<>();
        if (emailMap.size() != 0)
            emailMatches =
                    ofy(client).load().type(MemberEntity.class).filter("email in", emailMap.keySet()).list();

        List<MemberEntity> mobileMatches = new ArrayList<>();
        if (mobileMap.size() != 0) {
            mobileMatches =
                    ofy(client).load().type(MemberEntity.class).filter("mobilePhone in", mobileMap.keySet())
                            .list();
        }

        for (MemberEntity me : emailMatches) {
            MemberProp mp = me.toProp();
            String memberName = mp.contact.getName();

            for (Integer index : emailMap.get(mp.contact.email)) {
                String particantName = contactDetailProps.get(index).getName();

                if (Utils.closeEnough(memberName, particantName))
                    memberIds.set(index, mp.memberId);
            }
        }

        for (MemberEntity me : mobileMatches) {
            MemberProp mp = me.toProp();
            String memberName = mp.contact.getName();

            for (Integer index : mobileMap.get(mp.contact.mobilePhone)) {
                if (memberIds.get(index) == null) {
                    String particantName = contactDetailProps.get(index).getName();

                    if (Utils.closeEnough(memberName, particantName))
                        memberIds.set(index, mp.memberId);
                }
            }
        }

        return memberIds;
    }

    public static List<MemberEntity> getMatchingMembersSameSizeList(String client,
                                                                    List<ContactProp> contacts, String login) {

        // Returns a list same size as contacts
        // list element contains null when there is no match, else matching
        // member id

        Client.ensureValid(client);

        ensureNotNull(contacts, "contacts is null");

        Set<String> firstName3Chars = new HashSet<>();
        for (ContactProp c : contacts) {
            if ((c == null) || (c.firstName == null) || (c.firstName.length() < 3))
                continue;

            String firstName3Char = c.firstName.substring(0, 3).toLowerCase();

            firstName3Chars.add(firstName3Char);
        }

        MemberQueryCondition qc = new MemberQueryCondition(client, 10000);
        qc.firstName3Chars = firstName3Chars;

        List<Key<MemberEntity>> matchingMemberKeys = MemberLoader.queryKeys(qc, login);

        Iterable<MemberEntity> entities = ofy(client).load().keys(matchingMemberKeys).values();

        Map<String, Set<MemberEntity>> firstName3CharVsEntity = new HashMap<>(); // multimap

        for (MemberEntity entity : entities) {

            String firstName3Char = entity.firstName3Char;

            if (!firstName3CharVsEntity.containsKey(firstName3Char)) {
                firstName3CharVsEntity.put(firstName3Char, new HashSet<MemberEntity>());
            }

            firstName3CharVsEntity.get(firstName3Char).add(entity);
        }

        List<MemberEntity> matchingEntities = new ArrayList<>(contacts.size());
        for (ContactProp c : contacts) {
            if ((c == null) || (c.firstName == null) || (c.firstName.length() < 3)) {
                matchingEntities.add(null);
                continue;
            }

            String firstName3Char = c.firstName.substring(0, 3).toLowerCase();

            if (!firstName3CharVsEntity.containsKey(firstName3Char)) {
                // no matching member
                matchingEntities.add(null);
                continue;
            }

            Set<MemberEntity> set = firstName3CharVsEntity.get(firstName3Char);

            MemberEntity entity = findMatchingMember(c, set);
            matchingEntities.add(entity);
        }

        ensureEqual(contacts.size(), matchingEntities.size());

        return matchingEntities;
    }

    public static MemberEntity findMatchingMember(ContactProp contactProp,
                                                  Set<MemberEntity> memberEntities) {

        ensureNotNull(contactProp, "contactProp cannot be null");
        ensureNotNull(memberEntities, "memberEntities cannot be null");

        for (MemberEntity memberEntity : memberEntities) {
            if (Contact.isMatching(contactProp, memberEntity.toProp().contact))
                return memberEntity;
        }

        return null;
    }

    static boolean populateContactDetails(MemberEntity memberEntity, ContactProp contact) {

        // TODO: use a smarter home address comparison function

        DateUtils.ensureFormatYYYYMMDD(contact.asOfyyyymmdd);

        if (memberEntity.asOfYYYYMMDD > contact.asOfyyyymmdd)
            return false;

        boolean changed = false;

        if (contact.email != null) {
            Utils.ensureValidEmail(contact.email);

            if (Utils.isDifferentCaseInsensitive(memberEntity.email, contact.email)) {
                memberEntity.email = contact.email.toLowerCase();
                changed = true;
            }
        }

        if (contact.firstName != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.firstName, contact.firstName)) {
                memberEntity.firstName = contact.firstName;
                changed = true;
            }
        }

        if (contact.lastName != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.lastName, contact.lastName)) {
                memberEntity.lastName = contact.lastName;
                changed = true;
            }
        }

        if (contact.gender != null) {
            if (memberEntity.gender != contact.gender)
                changed = true;
            memberEntity.gender = contact.gender;
        }

        if (contact.homeAddress.address != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.homeAddress, contact.homeAddress.address)) {
                memberEntity.homeAddress = contact.homeAddress.address;
                changed = true;
            }
        }

        if (contact.homeAddress.city != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.homeCity, contact.homeAddress.city)) {
                memberEntity.homeCity = contact.homeAddress.city;
                changed = true;
            }
        }

        if (contact.homeAddress.state != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.homeState, contact.homeAddress.state)) {
                memberEntity.homeState = contact.homeAddress.state;
                changed = true;
            }
        }

        if (contact.homeAddress.pincode != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.homePincode, contact.homeAddress.pincode)) {
                memberEntity.homePincode = contact.homeAddress.pincode;
                changed = true;
            }
        }

        if (contact.homeAddress.country != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.homeCountry, contact.homeAddress.country)) {
                memberEntity.homeCountry = contact.homeAddress.country;
                changed = true;
            }
        }

        if (contact.officeAddress.address != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.officeAddress,
                    contact.officeAddress.address)) {
                memberEntity.officeAddress = contact.officeAddress.address;
                changed = true;
            }
        }

        if (contact.company != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.company, contact.company)) {
                memberEntity.company = contact.company;
                changed = true;
            }
        }

        if (contact.occupation != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.occupation,
                contact.occupation)) {
                memberEntity.occupation = contact.occupation;
                changed = true;
            }
        }

        if (contact.officeAddress.pincode != null) {
            if (Utils.isDifferentCaseInsensitive(memberEntity.officePincode,
                    contact.officeAddress.pincode)) {
                memberEntity.officePincode = contact.officeAddress.pincode;
                changed = true;
            }
        }

        if (contact.homePhone != null) {
            Utils.ensureValidPhoneNumber(contact.homePhone);
            if (Utils.isDifferentCaseInsensitive(memberEntity.homePhone, contact.homePhone)) {
                memberEntity.homePhone = contact.homePhone;
                changed = true;
            }
        }

        if (contact.officePhone != null) {
            Utils.ensureValidPhoneNumber(contact.officePhone);
            if (Utils.isDifferentCaseInsensitive(memberEntity.officePhone, contact.officePhone)) {
                memberEntity.officePhone = contact.officePhone;
                changed = true;
            }
        }

        if (contact.mobilePhone != null) {
            Utils.ensureValidPhoneNumber(contact.mobilePhone);
            if (Utils.isDifferentCaseInsensitive(memberEntity.mobilePhone, contact.mobilePhone)) {
                memberEntity.mobilePhone = contact.mobilePhone;
                changed = true;
            }
        }

        if (changed)
            memberEntity.asOfYYYYMMDD = contact.asOfyyyymmdd;

        return changed;
    }

    static void populateDependantFields(String client, List<MemberEntity> memberEntities) {

        ensureNotNull(memberEntities, "memberEntities cannot be null");

        for (MemberEntity memberEntity : memberEntities) {
            ensureNotNull(memberEntity, "individual memberEntity cannot be null");
            populateDependantFieldsWithoutDBQuery(memberEntity);
        }

        // fields that involve database queries
        List<Long> programIds = new ArrayList<>();
        for (MemberEntity memberEntity : memberEntities) {
            programIds.addAll(memberEntity.programIds);
        }

        Map<Long, ProgramProp> programProps = Program.getProps(client, programIds);

        for (MemberEntity memberEntity : memberEntities) {
            memberEntity.programTypeIds.clear();
            memberEntity.practiceIds.clear();

            for (long programId : memberEntity.programIds) {
                if (programProps.containsKey(programId)) {
                    ProgramProp programProp = programProps.get(programId);

                    memberEntity.programTypeIds.add(programProp.programTypeProp.programTypeId);
                }
            }

            for (Integer i : memberEntity.uvpMap.keySet()) {
                long programTypeId = memberEntity.uvpMap.get(i).programTypeId;
                memberEntity.programTypeIds.add(programTypeId);
            }

            memberEntity.practiceIds = ProgramType.getPracticeIds(client, memberEntity.programTypeIds);
            memberEntity.practiceIds.addAll(crmdna.list.List.getPracticeIds(client,
                    memberEntity.listIds));
        }
    }

    static MemberEntity populateDependantFieldsWithoutDBQuery(MemberEntity memberEntity) {
        // nameFirstChar
        if (memberEntity.firstName != null) {
            memberEntity.name = memberEntity.firstName.toLowerCase();
        } else if (memberEntity.email != null)
            memberEntity.name = memberEntity.email.toLowerCase();
        else if (memberEntity.mobilePhone != null)
            memberEntity.name = memberEntity.mobilePhone.toLowerCase();
        else if (memberEntity.homePhone != null)
            memberEntity.name = memberEntity.homePhone.toLowerCase();
        else if (memberEntity.officePhone != null)
            memberEntity.name = memberEntity.officePhone.toLowerCase();
        else {
            // should never come here as either email or a valid phone number
            // should be specified
            throw new APIException()
                    .status(Status.ERROR_RESOURCE_INCORRECT)
                    .message(
                            "Either email or a valid phone number should be specified when adding or updating member");
        }
        memberEntity.nameFirstChar = memberEntity.name.substring(0, 1);

        if ((memberEntity.firstName != null) && (memberEntity.firstName.length() > 2))
            memberEntity.firstName3Char = memberEntity.firstName.substring(0, 3).toLowerCase();

        memberEntity.qsTags =
                Utils.getQSTags(memberEntity.email, memberEntity.firstName, memberEntity.lastName,
                        memberEntity.homePhone, memberEntity.officePhone, memberEntity.mobilePhone);

        return memberEntity;
    }

    public static List<UnverifiedProgramProp> addUnverifiedProgram(String client, long memberId,
                                                                   long programTypeId, Month month, int year, String city, String country, String teacher,
                                                                   String login) {

        Client.ensureValid(client);

        MemberEntity entity = MemberLoader.safeGet(client, memberId, login);
        // this also checks if (login is same as member) or (login is a valid user)

        ProgramType.safeGet(client, programTypeId);

        ensure(year > 1970, "year should be after 1971");

        DateUtils.ensureDateNotInFuture(month, year);

        int unverifiedProgramId;
        if (entity.uvpMap.isEmpty()) {
            unverifiedProgramId = 1;
        } else {
            unverifiedProgramId = entity.uvpMap.lastEntry().getValue().unverifiedProgramId + 1;
        }

        UnverifiedProgramProp prop = new UnverifiedProgramProp();
        prop.unverifiedProgramId = unverifiedProgramId;
        prop.programTypeId = programTypeId;
        prop.month = month;
        prop.year = year;
        prop.teacher = teacher;
        prop.city = city;
        prop.country = country;

        entity.uvpMap.put(unverifiedProgramId, prop);

        Member.populateDependantFields(client, Utils.getList(entity));
        ofy(client).save().entity(entity).now();

        Member.populateDependantFields(client, Utils.getList(entity));

        List<UnverifiedProgramProp> list = new ArrayList<>(entity.uvpMap.values());

        Collections.sort(list);
        return list;
    }

    public static List<UnverifiedProgramProp> deleteUnverifiedProgram(String client, long memberId,
                                                                      int unverifiedProgramTypeId, String login) {

        Client.ensureValid(client);

        MemberEntity entity = MemberLoader.safeGet(client, memberId, login);
        // this also checks if (login is same as member) or (login is a valid user)

        if (!entity.uvpMap.containsKey(unverifiedProgramTypeId))
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "No unverified program [" + unverifiedProgramTypeId + "] for member [" + memberId + "]");

        entity.uvpMap.remove(unverifiedProgramTypeId);
        populateDependantFields(client, Utils.getList(entity));

        ofy(client).save().entity(entity).now();

        List<UnverifiedProgramProp> list = new ArrayList<>(entity.uvpMap.values());

        Collections.sort(list);
        return list;
    }

    public static void rebuild(MemberQueryCondition mqc, String login) {

        ensureNotNull(mqc, "mqc is null");
        Client.ensureValid(mqc.client);

        User.ensureValidUser(mqc.client, login);

        final int MAX_MEMBERS = 10000;
        int count = MemberLoader.getCount(mqc, login);
        if (count > MAX_MEMBERS)
            throw new APIException("Attempt to rebuild [" + count + "] member entities. Max allowed is ["
                    + MAX_MEMBERS + "]").status(Status.ERROR_OVERFLOW);

        List<MemberEntity> memberEntities = MemberLoader.queryEntities(mqc, login);

        rebuild(mqc.client, memberEntities);
    }

    private static void rebuild(String client, List<MemberEntity> memberEntities) {
        Client.ensureValid(client);
        ensureNoNullElement(memberEntities);

        removeDeadReferences(client, memberEntities);
        populateDependantFields(client, memberEntities);

        ofy(client).save().entities(memberEntities);
    }

    private static void removeDeadReferences(String client, List<MemberEntity> memberEntities) {

        Client.ensureValid(client);
        ensureNoNullElement(memberEntities);

        Set<Long> programIds = new HashSet<>();
        Set<Long> listIds = new HashSet<>();
        for (MemberEntity memberEntity : memberEntities) {
            programIds.addAll(memberEntity.programIds);
            listIds.addAll(memberEntity.listIds);
        }

        Map<Long, ProgramEntity> programMap = Program.getEntities(client, programIds);
        Map<Long, ListEntity> listMap = crmdna.list.List.get(client, listIds);

        Set<Long> missingProgramIds = programIds;
        missingProgramIds.removeAll(programMap.keySet());

        Set<Long> missingListIds = listIds;
        missingListIds.removeAll(listMap.keySet());

        for (MemberEntity memberEntity : memberEntities) {
            memberEntity.programIds.removeAll(missingProgramIds);
            memberEntity.listIds.removeAll(missingListIds);
        }
    }

    public static BulkSubscriptionResultProp bulkSubscribeList(String client, long listId,
                                                               MailMap mailMap, String login) {

        //This will add the emails to list and subscribe them to group provided they are not unsubscribed.

        Client.ensureValid(client);
        ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

        Group.safeGet(client, listProp.groupId);

        if (!listProp.enabled)
            throw new APIException("Cannot add emails to disabled list [" + listId + "]")
                    .status(Status.ERROR_PRECONDITION_FAILED);

        ensureNotNull(mailMap, "mailMap is null");
        User.ensureGroupLevelPrivilege(client, listProp.groupId, login, GroupLevelPrivilege.UPDATE_LIST);

        ensure(mailMap.size() != 0, "mailMap is empty");
        ensure(mailMap.size() < 5001,
            "Attempt to add [" + mailMap.size() + "] emails to list [" + listId
                + "] in one shot. Max allowed in one shot is [5000]. Please split up into multiple batches");

        Set<String> allEmails = mailMap.getEmails();

        List<MemberEntity> existingMemberEntities =
                ofy(client).load().type(MemberEntity.class).filter("email in", allEmails).list();

        BulkSubscriptionResultProp result = new BulkSubscriptionResultProp();

        for (MemberEntity memberEntity : existingMemberEntities) {
            ensureNotNull(memberEntity.email);
            result.existingMemberEmails.add(memberEntity.email);
        }

        result.newMemberEmails.addAll(allEmails);
        result.newMemberEmails.removeAll(result.existingMemberEmails);

        List<MemberEntity> toSave = new ArrayList<>();
        for (MemberEntity memberEntity : existingMemberEntities) {
            ensureNotNull(memberEntity.email);

            boolean changed = memberEntity.listIds.add(listId);
            if (changed) {
                toSave.add(memberEntity);
                result.addedToListEmails.add(memberEntity.email);
            }

            //subscribe to group if not already unsubscribed
            if (! memberEntity.unsubscribedGroupIds.contains(listProp.groupId)) {
                boolean changed2 = memberEntity.subscribedGroupIds.add(listProp.groupId);
                if (changed2) {
                    toSave.add(memberEntity);
                    result.addedToGroupSubscriptionEmails.add(memberEntity.email);
                } else {
                    result.alreadySubscribedToGroupEmails.add(memberEntity.email);
                }
            } else {
                result.alreadyUnsubscribedToGroupEmails.add(memberEntity.email);
            }
        }

        ensure(allEmails.size() >= result.newMemberEmails.size());

        List<MemberEntity> newMemberEntities = MemberFactory.create(client, result.newMemberEmails.size());
        ensureEqual(result.newMemberEmails.size(), newMemberEntities.size());
        int i = 0;
        for (String email : result.newMemberEmails) {
            MemberEntity memberEntity = newMemberEntities.get(i);
            memberEntity.email = email.toLowerCase();
            memberEntity.firstName = mailMap.get(MailMap.MergeVarID.FIRST_NAME, email);
            memberEntity.lastName = mailMap.get(MailMap.MergeVarID.LAST_NAME, email);
            memberEntity.groupIds.add(listProp.groupId);
            memberEntity.asOfYYYYMMDD = DateUtils.toYYYYMMDD(new Date());
            memberEntity.listIds.add(listId);
            memberEntity.subscribedGroupIds.add(listProp.groupId);

            result.addedToListEmails.add(email);
            result.addedToGroupSubscriptionEmails.add(email);

            toSave.add(memberEntity);
            result.newMemberEmails.add(email);
            i = i + 1;
        }

        populateDependantFields(client, toSave);

        ofy(client).save().entities(toSave).now();

        return result;
    }

    public static String getCSV(String client, List<MemberProp> memberProps) {

        Client.ensureValid(client);

        StringBuilder sb = new StringBuilder();
        sb.append("First Name, Last Name, Email, Mobile, Home Phone, Office Phone, Occupation, "
            + "Company Name, Programs, Practices, Member ID\n");

        if (memberProps == null || memberProps.isEmpty())
            return sb.toString();

        for (MemberProp memberProp : memberProps) {
            sb.append(Utils.csvEncode(memberProp.contact.firstName)).append(',');
            sb.append(Utils.csvEncode(memberProp.contact.lastName)).append(',');
            sb.append(Utils.csvEncode(memberProp.contact.email)).append(',');
            sb.append(Utils.csvEncode(memberProp.contact.mobilePhone)).append(',');
            sb.append(Utils.csvEncode(memberProp.contact.homePhone)).append(',');
            sb.append(Utils.csvEncode(memberProp.contact.officePhone)).append(',');
            sb.append(Utils.csvEncode(memberProp.contact.occupation)).append(',');
            sb.append(Utils.csvEncode(memberProp.contact.company)).append(',');

            StringBuilder sbPrograms = new StringBuilder();
            for (Iterator<MemberProgramProp> iter = memberProp.memberProgramProps.iterator(); iter.hasNext(); ) {
                MemberProgramProp memberProgramProp = iter.next();
                if (memberProgramProp.programType.contains("Sathsang")) continue;

                if (!memberProgramProp.verified) sb.append('<');

                sbPrograms.append(memberProgramProp.programType.replaceAll(" ", "")).append('_');
                sbPrograms.append(memberProgramProp.month).append('-');
                sbPrograms.append(memberProgramProp.year).append('-');

                if (memberProgramProp.teacher != null) {
                    sbPrograms.append(memberProgramProp.teacher.replaceAll(" ", ""));
                } else {
                    sbPrograms.append("NA");
                }

                if (!memberProgramProp.verified) sbPrograms.append('>');

                if (iter.hasNext()) sbPrograms.append("\r\n");
            }
            sb.append(Utils.csvEncode(sbPrograms.toString())).append(',');

            StringBuilder sbPractices = new StringBuilder();
            for (Iterator<String> iter = memberProp.practices.iterator(); iter.hasNext(); ) {
                String practice = iter.next();
                sbPractices.append(practice.replaceAll(" ", ""));

                if (iter.hasNext()) sbPractices.append("\r\n");
            }
            sb.append(Utils.csvEncode(sbPractices.toString())).append(',');

            sb.append(memberProp.memberId);
            sb.append("\r\n");
        }

        return sb.toString();
    }

    //This is used for datamigration for group level subscription. This method should probably be removed after data migration is complete
    //returns the number of entities that changed
    public static Set<Long> populateSubUnsubGroupIds(String client, Set<Long> memberIds, String login) {

        Client.ensureValid(client);
        ensure(UserCore.isSuperUser(login), "This function can be called only by a super user");

        Map<Long, MemberEntity> memberIdVsEntity = MemberLoader.get(client, memberIds, login);

        Set<Long> allListIds = new HashSet<>();
        for (Map.Entry<Long, MemberEntity> e : memberIdVsEntity.entrySet()) {
            allListIds.addAll(e.getValue().toProp().subscribedListIds);
            allListIds.addAll(e.getValue().toProp().unsubscribedListIds);
        }

        Map<Long, ListEntity> listIdVsEntity = crmdna.list.List.get(client, allListIds);

        List<MemberEntity> toSave = new ArrayList<>();
        Set<Long> changedMemberIds = new HashSet<>();
        for (Map.Entry<Long, MemberEntity> e : memberIdVsEntity.entrySet()) {
            MemberEntity memberEntity = e.getValue();
            boolean changed = false;
            Set<Long> subscribedGroupIds = new HashSet<>();
            for (long listId : memberEntity.subscribedListIds) {
                if (listIdVsEntity.containsKey(listId)) {
                    long groupId = listIdVsEntity.get(listId).toProp().groupId;
                    subscribedGroupIds.add(groupId);
                }
            }

            Set<Long> unsubscribedGroupIds = new HashSet<>();
            for (long listId : memberEntity.unsubscribedListIds) {
                if (listIdVsEntity.containsKey(listId)) {
                    long groupId = listIdVsEntity.get(listId).toProp().groupId;
                    unsubscribedGroupIds.add(groupId);
                    //remove subscription if any
                    subscribedGroupIds.remove(groupId);
                }
            }

            if (memberEntity.subscribedGroupIds.addAll(subscribedGroupIds)) {
                changed = true;
            }

            if (memberEntity.unsubscribedGroupIds.addAll(unsubscribedGroupIds)) {
                changed = true;
            }

            if (memberEntity.subscribedGroupIds.removeAll(memberEntity.unsubscribedGroupIds)) {
                changed = true;
            }

            if (memberEntity.listIds.addAll(memberEntity.subscribedListIds)) {
                changed = true;
            }

            if (memberEntity.listIds.addAll(memberEntity.unsubscribedListIds)) {
                changed = true;
            }

            if (changed) {
                toSave.add(memberEntity);
                changedMemberIds.add(memberEntity.getId());
            }
        }

        ofy(client).save().entities(toSave).now();

        return changedMemberIds;
    }

    public enum AccountType {
        FEDERATED, GOOGLE
    }
}
