package crmdna.member;

import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.program.Program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;

// TODO: consider renaming this as bulk updater
public class MemberBulkSaver {
    private String client;

    private List<MemberEntity> entities = new ArrayList<>();

    private List<ContactProp> contacts = new ArrayList<>();

    private Set<Long> programIdsToAdd = new HashSet<>();
    private Set<Long> programIdsToDelete = new HashSet<>();

    private Set<Long> groupIdsToAdd = new HashSet<>();
    private Set<Long> groupIdsToDelete = new HashSet<>();

    private MemberBulkSaver() {
    }

    public static MemberBulkSaver memberBulkSaver(String client, List<MemberEntity> entities) {
        Client.ensureValid(client);
        ensureNotNull(entities, "entities cannot be null");

        MemberBulkSaver saver = new MemberBulkSaver();
        saver.client = client;
        saver.entities = entities;

        return saver;
    }

    public MemberBulkSaver addProgramToAll(long programId) {

        Program.safeGet(client, programId);
        programIdsToAdd.add(programId);
        programIdsToDelete.remove(programId);

        return this;
    }

    public MemberBulkSaver deleteProgramFromAll(long programId) {
        programIdsToAdd.remove(programId);
        programIdsToDelete.add(programId);

        return this;
    }

    public MemberBulkSaver addGroupToAll(long groupId) {
        Group.safeGet(client, groupId);
        groupIdsToAdd.add(groupId);
        groupIdsToDelete.remove(groupId);

        return this;
    }

    public MemberBulkSaver deleteGroupToAll(long groupId) {
        groupIdsToAdd.remove(groupId);
        groupIdsToDelete.add(groupId);

        return this;
    }

    public MemberBulkSaver setContactsSameSizeList(List<ContactProp> contacts) {

        ensureNotNull(contacts, "contacts cannot be null");
        ensureEqual(entities.size(), contacts.size(), "No of contacts [" + contacts.size()
                + "] does not match no of entities [" + entities.size() + "]");

        // ensure contact is not null
        for (int i = 0; i < contacts.size(); i++) {
            ContactProp contact = contacts.get(i);
            ensureNotNull(contact, "contact(" + i + ") is null");
        }

        this.contacts = contacts;

        return this;
    }

    public MemberBulkSaver populateDependantsAndSave() {

        if (!contacts.isEmpty()) {
            ensureEqual(entities.size(), contacts.size());
        }

        Set<Integer> changedIndexes = new HashSet<>();
        for (int i = 0; i < entities.size(); i++) {
            MemberEntity entity = entities.get(i);

            if (entity == null)
                continue;

            boolean change = entity.programIds.addAll(programIdsToAdd);
            if (change)
                changedIndexes.add(i);

            change = entity.programIds.removeAll(programIdsToDelete);
            if (change)
                changedIndexes.add(i);

            change = entity.groupIds.addAll(groupIdsToAdd);
            if (change)
                changedIndexes.add(i);

            change = entity.groupIds.removeAll(groupIdsToDelete);
            if (change)
                changedIndexes.add(i);

            if (entity.groupIds.size() == 0) {
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Member [" + entity.getId() + "] should have atleast one group");
            }

            if (!contacts.isEmpty()) {
                ContactProp contact = contacts.get(i);
                ensureNotNull(contact, "contact(" + i + ") is null");

                change = Member.populateContactDetails(entity, contact);

                if (change)
                    changedIndexes.add(i);
            }
        }

        ensure(changedIndexes.size() <= entities.size());

        List<MemberEntity> changedEntities = new ArrayList<>();
        for (int i : changedIndexes) {
            MemberEntity entity = entities.get(i);
            changedEntities.add(entity);
        }

        Member.populateDependantFields(client, changedEntities);

        ofy(client).save().entities(changedEntities);
        return this;
    }
}
