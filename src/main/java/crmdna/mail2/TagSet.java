package crmdna.mail2;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNoNullElement;
import static crmdna.common.OfyService.ofy;

class TagSet {

    public static TagSetProp getIfExistsElseCreateAndGet(String client, Set<String> tags) {
        Client.ensureValid(client);

        ensureNoNullElement(tags);
        ensure(!tags.isEmpty(), "tags is empty");

        tags = sanitize(tags); // convert to lower case

        Query<TagSetEntity> query = ofy(client).load().type(TagSetEntity.class);
        for (String tag : tags) {
            query = query.filter("tags", tag);
        }

        List<TagSetEntity> tagSetEntities = query.list();

        if (tagSetEntities.isEmpty()) {
            TagSetEntity tagSetEntity = new TagSetEntity();
            tagSetEntity.tagSetId = Sequence.getNext(client, SequenceType.TAGSET);
            tagSetEntity.tags = tags;
            ofy(client).save().entity(tagSetEntity);

            return tagSetEntity.toProp();
        }

        if (tagSetEntities.size() > 1) {
            // TODO: send alert email to development team
        }

        return tagSetEntities.get(0).toProp();
    }

    public static TagSetEntity safeGet(String client, long tagSetId) {
        Client.ensureValid(client);

        TagSetEntity entity = ofy(client).load().type(TagSetEntity.class).id(tagSetId).now();

        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Tag set id [" + tagSetId + "] not found for client [" + client + "]");

        return entity;
    }


    public static List<TagSetEntity> query(String client, Set<String> tags) {
        Client.ensureValid(client);

        ensureNoNullElement(tags);

        if (tags.isEmpty())
            return new ArrayList<>();

        tags = sanitize(tags); // convert to lower case

        Query<TagSetEntity> query = ofy(client).load().type(TagSetEntity.class);
        for (String tag : tags) {
            query = query.filter("tags", tag);
        }

        return query.list();
    }


    static Set<String> sanitize(Set<String> tags) {
        ensureNoNullElement(tags);

        Set<String> lcase = new HashSet<>(tags.size());
        for (String tag : tags) {
            lcase.add(tag.toLowerCase());
        }

        return lcase;
    }
}
