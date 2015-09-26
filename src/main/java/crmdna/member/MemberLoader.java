package crmdna.member;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.group.GroupHelper;
import crmdna.list.ListEntity;
import crmdna.list.ListHelper;
import crmdna.practice.PracticeHelper;
import crmdna.program.Program;
import crmdna.program.ProgramEntity;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramTypeHelper;
import crmdna.user.User;

import java.util.*;
import java.util.Map.Entry;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class MemberLoader {

    public static final int MAX_RESULT_SIZE = 2500;

    public static MemberEntity safeGet(String client, long memberId, String login) {

        ensure(memberId != 0, "memberId is 0");
        Client.ensureValid(client);
        ensureNotNull(login, "login is null");

        MemberEntity entity = ofy(client).load().type(MemberEntity.class).id(memberId).now();

        if (null == entity)
            throw new APIException("There is no member with id [" + memberId + "]")
                .status(Status.ERROR_RESOURCE_NOT_FOUND);

        if ((entity.email == null) || !entity.email.equalsIgnoreCase(login))
            User.ensureValidUser(client, login);

        return entity;
    }

    public static MemberEntity safeGetByIdOrEmail(String client, String memberIdOrEmail,
        String login) {

        if (Utils.canParseAsLong(memberIdOrEmail)) {
            return safeGet(client, Utils.safeParseAsLong(memberIdOrEmail), login);
        } else {
            String email = memberIdOrEmail.toLowerCase();
            Utils.ensureValidEmail(email);
            MemberQueryCondition qc = new MemberQueryCondition(client, 100);
            qc.email = email;
            List<MemberEntity> entities = MemberLoader.queryEntities(qc, User.SUPER_USER);
            ensure(!entities.isEmpty(), "There is no member with email [" + email + "]");

            ensure(entities.size() == 1,
                "There are [" + entities.size() + "] members with email [" + email
                    + "]. Specify memberId");

            ensure(email.equals(entities.get(0).email));
            return entities.get(0);
        }
    }

    public static MemberEntity getByEmail(String client, String email) {

        Utils.ensureValidEmail(email);
        MemberQueryCondition qc = new MemberQueryCondition(client, 100);
        qc.email = email;
        List<MemberEntity> entities = MemberLoader.queryEntities(qc, User.SUPER_USER);

        if (entities.isEmpty())
            return null;

        return entities.get(0);
    }

    public static List<MemberProp> quickSearch(String client, String searchStr, Set<Long> groupIds,
        int maxResultSize, String login) {

        ensure(maxResultSize > 0, "invalid maxResultSize [" + maxResultSize + "]");

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        MemberQueryCondition qc = new MemberQueryCondition(client, maxResultSize);
        qc.searchStr = searchStr;
        qc.groupIds = groupIds;

        List<MemberProp> memberProps = MemberLoader.querySortedProps(qc, login);

        PracticeHelper.populateName(client, memberProps);
        GroupHelper.populateName(client, memberProps);

        // TODO: use projection query
        for (MemberProp memberProp : memberProps) {
            memberProp.contact.homeAddress = null;
            memberProp.contact.officeAddress = null;
        }

        return memberProps;
    }

    public static MemberProp safeGetDetailedInfo(String client, long memberId, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        MemberProp memberProp = memberEntity.toProp();
        populateDependents(client, memberProp, login);

        return memberProp;
    }

    public static void populateDependents(String client, MemberProp memberProp, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberProp.memberId, login);

        // populate programProps and practices
        PracticeHelper.populateName(client, Utils.getList(memberProp));
        GroupHelper.populateName(client, Utils.getList(memberProp));

        // both verified and unverified programs go into memberprogramprop
        Map<Long, ProgramEntity> map = Program.getEntities(client, memberProp.programIds);

        for (Long programId : map.keySet()) {
            ProgramProp programProp = map.get(programId).toProp(client);

            MemberProgramProp memberProgramProp = new MemberProgramProp();
            memberProgramProp.groupOrCity = programProp.groupProp.displayName;
            memberProgramProp.month = DateUtils.getMonthEnum(programProp.startYYYYMMDD);
            memberProgramProp.programTypeId = programProp.programTypeProp.programTypeId;
            memberProgramProp.teacher =
                programProp.teacherProp.firstName + " " + programProp.teacherProp.lastName;
            memberProgramProp.verified = true;
            memberProgramProp.year = programProp.startYYYYMMDD / 10000;
            memberProp.memberProgramProps.add(memberProgramProp);
        }

        for (Integer i : memberEntity.uvpMap.keySet()) {
            UnverifiedProgramProp unverifiedProgramProp = memberEntity.uvpMap.get(i);
            MemberProgramProp memberProgramProp = new MemberProgramProp();
            memberProgramProp.groupOrCity = unverifiedProgramProp.city;
            memberProgramProp.month = unverifiedProgramProp.month;
            memberProgramProp.programTypeId = unverifiedProgramProp.programTypeId;
            memberProgramProp.teacher = unverifiedProgramProp.teacher;
            memberProgramProp.verified = false;
            memberProgramProp.year = unverifiedProgramProp.year;
            memberProgramProp.unverifiedProgramId = unverifiedProgramProp.unverifiedProgramId;

            memberProp.memberProgramProps.add(memberProgramProp);
        }

        ProgramTypeHelper.populateName(client, memberProp.memberProgramProps);

        Collections.sort(memberProp.memberProgramProps);

        Map<Long, ListEntity> listEntityMap = crmdna.list.List.get(client, memberProp.listIds);
        for (Entry<Long, ListEntity> entry : listEntityMap.entrySet()) {
            memberProp.listProps.add(entry.getValue().toProp());
        }

        Collections.sort(memberProp.listProps);
        ListHelper.populateGroupName(client, memberProp.listProps);
    }

    public static Map<Long, MemberEntity> get(String client, Iterable<Long> memberIds,
        String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        return ofy(client).load().type(MemberEntity.class).ids(memberIds);
    }

    public static List<MemberProp> querySortedProps(MemberQueryCondition condition, String login) {

        List<MemberProp> props = queryProps(condition, login);

        Collections.sort(props);

        return props;
    }

    public static List<MemberProp> queryWithCursor(MemberQueryCondition condition, String login) {

        Client.ensureValid(condition.client);
        User.ensureValidUser(condition.client, login);
        boolean proceed = false;

        List<MemberProp> props = new ArrayList<>();

        Query<MemberEntity> q = getQuery(condition);
        QueryResultIterator<MemberEntity> iterator = q.iterator();
        while (iterator.hasNext()) {
            MemberEntity memberEntity = iterator.next();
            MemberProp memberProp = memberEntity.toProp();
            MemberLoader.populateDependents(condition.client, memberProp, login);
            props.add(memberProp);
            proceed = true;
        }

        condition.cursor = proceed ? iterator.getCursor().toWebSafeString() : null;

        Collections.sort(props);

        return props;
    }

    public static List<MemberProp> queryProps(MemberQueryCondition condition, String login) {

        Collection<MemberEntity> entities = queryEntities(condition, login);

        List<MemberProp> props = new ArrayList<>(entities.size());
        for (MemberEntity memberEntity : entities) {
            props.add(memberEntity.toProp());
        }

        return props;
    }

    public static List<MemberEntity> queryEntities(MemberQueryCondition condition, String login) {

        List<Key<MemberEntity>> memberKeys = queryKeys(condition, login);

        Map<Key<MemberEntity>, MemberEntity> map = ofy(condition.client).load().keys(memberKeys);

        return new ArrayList<>(map.values());
    }

    static List<Key<MemberEntity>> queryKeys(MemberQueryCondition condition, String login) {

        Client.ensureValid(condition.client);
        User.ensureValidUser(condition.client, login);

        Query<MemberEntity> q = getQuery(condition);

        List<Key<MemberEntity>> memberKeys = q.keys().list();

        if ((condition.maxResultSize != null) && (memberKeys.size() > condition.maxResultSize)) {
            throw new APIException().status(Status.ERROR_OVERFLOW).message(
                "Found [" + memberKeys.size() + "] matches. (Max allowed is ["
                    + condition.maxResultSize + "])");
        }

        return memberKeys;
    }

    public static int getCount(MemberQueryCondition condition, String login) {

        Client.ensureValid(condition.client);
        User.ensureValidUser(condition.client, login);

        Query<MemberEntity> q = getQuery(condition);

        return q.count();
    }

    private static Query<MemberEntity> getQuery(MemberQueryCondition condition) {

        Client.ensureValid(condition.client);

        ensureNotNull(condition, "MemberLoadCondition cannot be null");

        Query<MemberEntity> q = ofy(condition.client).load().type(MemberEntity.class);

        if ((null != condition.groupIds) && (condition.groupIds.size() != 0))
            q = q.filter("groupIds in", condition.groupIds);

        if ((null != condition.practiceIds) && (condition.practiceIds.size() != 0))
            q = q.filter("practiceIds in", condition.practiceIds);

        if ((null != condition.programTypeIds) && (condition.programTypeIds.size() != 0))
            q = q.filter("programTypeIds in", condition.programTypeIds);

        if ((null != condition.programIds) && (condition.programIds.size() != 0))
            q = q.filter("programIds in", condition.programIds);

        if ((null != condition.firstName3Chars) && (condition.firstName3Chars.size() != 0))
            q = q.filter("firstName3Char in", condition.firstName3Chars);

        if (null != condition.email)
            q = q.filter("email", condition.email.toLowerCase());

        if (null != condition.searchStr) {
            Set<String> searchTags = Utils.getQSTags(condition.searchStr);
            if (searchTags.isEmpty())
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT)
                    .message("Atleast one word in the search string should be minimum 3 chars");

            for (String tag : searchTags) {
                q = q.filter("qsTags", tag);
            }
        }

        if (null != condition.hasAccount)
            q = q.filter("hasAccount", condition.hasAccount);

        if ((null != condition.listIds) && !condition.listIds.isEmpty())
            q = q.filter("listIds in", condition.listIds);

        if ((null != condition.subscribedGroupIds) && !condition.subscribedGroupIds.isEmpty())
            q = q.filter("subscribedGroupIds in", condition.subscribedGroupIds);

        if ((null != condition.unsubscribedGroupIds) && !condition.unsubscribedGroupIds.isEmpty())
            q = q.filter("unsubscribedGroupIds in", condition.unsubscribedGroupIds);

        if (null != condition.nameFirstChar && condition.nameFirstChar.length() != 0)
            q = q.filter("nameFirstChar", condition.nameFirstChar);

        if (null != condition.cursor)
            q = q.startAt(Cursor.fromWebSafeString(condition.cursor));

        if (null != condition.maxResultSize)
            q = q.limit(condition.maxResultSize);

        return q;
    }

    public static TreeSet<String> getUnsubscribedEmails(String client, long groupId, String login) {

        Client.ensureValid(client);
        Group.safeGet(client, groupId);
        User.ensureValidUser(client, login);

        List<MemberEntity> memberEntities = ofy(client).load().type(MemberEntity.class)
                .filter("unsubscribedGroupIds", groupId)
                .project("email").list();

        TreeSet<String> emails = new TreeSet<>();

        for (MemberEntity memberEntity : memberEntities) {
            if (memberEntity.email != null) {
                emails.add(memberEntity.email);
            }
        }

        return emails;
    }
}
