package crmdna.venue;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.AssertUtils.ensureNotNullNotEmpty;
import static crmdna.common.OfyService.ofy;

public class Venue {

    public static VenueProp create(String client, String displayName, String shortName,
        String address, long groupId, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_VENUE);
        Group.safeGet(client, groupId);

        ensureNotNullNotEmpty(displayName, "displayName");
        ensureNotNullNotEmpty(address, "address");
        ensureNotNullNotEmpty(shortName, "shortName");

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());

        List<Key<VenueEntity>> keys =
                ofy(client).load().type(VenueEntity.class).filter("name", name).keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a venue with name [" + displayName + "]");

        String key = getUniqueKey(client, name);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a venue with name [" + displayName + "]");

        VenueEntity entity = new VenueEntity();
        entity.venueId = Sequence.getNext(client, SequenceType.VENUE);
        entity.name = name;
        entity.displayName = displayName;
        entity.shortName = shortName;
        entity.address = address;
        entity.groupId = groupId;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    private static String getUniqueKey(String namespace, String name) {
        return namespace + "_" + SequenceType.VENUE + "_" + name;
    }

    public static VenueEntity safeGet(String client, long venueId) {

        Client.ensureValid(client);

        VenueEntity entity = ofy(client).load().type(VenueEntity.class).id(venueId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Venue [" + venueId + "] does not exist");
        return entity;
    }

    public static VenueEntity safeGetByIdOrName(String client, String idOrName) {

        Client.ensureValid(client);
        ensureNotNull(idOrName);

        if (Utils.canParseAsLong(idOrName)) {
            long venueId = Utils.safeParseAsLong(idOrName);
            return safeGet(client, venueId);
        }

        idOrName = Utils.removeSpaceUnderscoreBracketAndHyphen(idOrName.toLowerCase());
        List<VenueEntity> entities =
                ofy(client).load().type(VenueEntity.class).filter("name", idOrName).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Venue [" + idOrName + "] does not exist");

        if (entities.size() > 1)
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT)
                    .message(
                            "Found [" + entities.size() + "] matches for venue [" + idOrName
                                    + "]. Please specify Id");
        return entities.get(0);
    }

    public static VenueProp update(final String client, final long venueId,
        final String newDisplayName, final String newShortName, final String newAddress,
        final Long newGroupId, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_VENUE);

        if (newGroupId != null)
            Group.safeGet(client, newGroupId);

        ensureValid(newDisplayName, newAddress);

        VenueEntity entity = safeGet(client, venueId);

        if (newDisplayName != null) {
            String newName = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());

            // if name is changing ensure it is unique
            if (!entity.name.equals(newName)) {
                List<Key<VenueEntity>> keys =
                        ofy(client).load().type(VenueEntity.class).filter("name", newName).keys().list();
                if (keys.size() != 0)
                    throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                            "There is already a venue with name [" + newDisplayName + "]");

                String key = getUniqueKey(client, newName);
                long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

                if (val != 1)
                    throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                            "There is already a venue (in cache) with name [" + newDisplayName + "]");
            }
        }

        VenueProp venueProp = ofy(client).transact(new Work<VenueProp>() {

            @Override
            public VenueProp run() {
                VenueEntity entity = safeGet(client, venueId);
                if (newDisplayName != null) {
                    entity.displayName = newDisplayName;
                    entity.name = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());
                }

                if (newShortName != null)
                    entity.shortName = newShortName;
                if (newAddress != null)
                    entity.address = newAddress;
                if (newGroupId != null)
                    entity.groupId = newGroupId;

                ofy(client).save().entity(entity).now();
                return entity.toProp();
            }
        });

        return venueProp;
    }

    private static void ensureValid(String displayName, String address) {
        if (displayName != null)
            if (displayName.equals(""))
                Utils.throwIncorrectSpecException("venue display name is empty string");

        if (address != null)
            if (address.equals(""))
                Utils.throwIncorrectSpecException("venue address is empty string");
    }

    public static List<VenueProp> getAll(String client) {

        Client.ensureValid(client);

        List<VenueEntity> entities = ofy(client).load().type(VenueEntity.class).order("name").list();

        List<VenueProp> props = new ArrayList<>();
        for (VenueEntity entity : entities)
            props.add(entity.toProp());

        Collections.sort(props);

        return props;
    }

    public static List<VenueProp> getAllForGroup(String client, long groupId) {
        Client.ensureValid(client);
        Group.safeGet(client, groupId);

        List<VenueEntity> entities =
                ofy(client).load().type(VenueEntity.class).filter("groupId", groupId).list();

        List<VenueProp> props = new ArrayList<>();
        for (VenueEntity entity : entities)
            props.add(entity.toProp());

        Collections.sort(props);

        return props;
    }

    public static void delete(String client, long venueId, String login) {
        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_VENUE);

        // TODO - check other references
        throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                "deleting venue not yet implemented");
    }

    public static class VenueProp implements Comparable<VenueProp> {
        public long venueId;
        public String name;
        public String displayName;
        public String address;
        public long groupId;
        public String shortName;

        @Override
        public int compareTo(VenueProp arg0) {
            return name.compareTo(arg0.name);
        }
    }
}
