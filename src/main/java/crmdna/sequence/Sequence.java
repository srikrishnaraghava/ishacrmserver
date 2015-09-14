package crmdna.sequence;

import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    public static long getNext(final String client, SequenceType sequenceType) {
        return getNext(client, sequenceType, 1).get(0);
    }

    public static long getNextAtGroupLevel(final String client, long groupId, GroupLevelSequenceType sequenceType) {
        return getNextAtGroupLevel(client, groupId, sequenceType, 1).get(0);
    }

    public static List<Long> getNext(final String client, final SequenceType type,
                                     final int numElements) {

        Client.ensureValid(client);

        if (numElements == 0)
            return new ArrayList<>();

        if (type == null)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "type cannot be null when calling getNext in Sequence");

        return SequenceCore.getNext(client, type.toString(), numElements);
    }

    public static List<Long> getNextAtGroupLevel(final String client,
        final long groupId, final GroupLevelSequenceType type, final int numElements) {

        Client.ensureValid(client);

        if (numElements == 0)
            return new ArrayList<>();

        if (type == null)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "type cannot be null when calling getNext in Sequence");

        return SequenceCore.getNext(client, type.toString() + "." + groupId, numElements);
    }

    public enum SequenceType {
        MEMBER, VENUE, TEACHER, PRACTICE, GROUP, USER, PROGRAM_TYPE,
        PROGRAM, PARTICIPANT, REGISTRANT, LIST, INVENTORY_ITEM_TYPE,
        INVENTORY_ITEM, DEPARTMENT, INTERACTION, URL, TAGSET, MAIL_CONTENT,
        INVENTORY_CHECKIN, INVENTORY_CHECKOUT, SESSIONPASS, PAYMENT, SUBSCRIPTION,
        PACKAGED_INVENTORY_ITEM, INVENTORY_LOCATION, INVENTORY_TRANSFER, PACKAGED_INVENTORY_SALES,
        PACKAGED_INVENTORY_BATCH, MAIL_SCHEDULE, DISCOUNT, CAMPAIGN
    }

    public enum GroupLevelSequenceType {
        RECEIPT
    }
}
