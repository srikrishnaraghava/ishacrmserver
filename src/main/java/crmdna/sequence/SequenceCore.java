package crmdna.sequence;

import com.googlecode.objectify.Work;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static crmdna.common.OfyService.ofy;

class SequenceCore {

    static List<Long> getNext(final String client, final String type, final int numElements) {

        Client.ensureValid(client);

        if (numElements < 1)
            throw new APIException(
                    "numElements should be > 0 when calling getNext in Sequence. Current value: ["
                            + numElements + "]").status(Status.ERROR_RESOURCE_INCORRECT);

        if ((type == null) || (type.equals("")))
            throw new APIException("Specified type [" + type
                    + "] is either null or empty string when calling Sequence.getNext")
                    .status(Status.ERROR_RESOURCE_INCORRECT);

        List<Long> next = ofy(client).transact(new Work<List<Long>>() {

            @Override
            public List<Long> run() {
                String id = type.toUpperCase();
                SequenceEntity sequenceEntity = ofy(client).load().type(SequenceEntity.class).id(id).now();

                if (null == sequenceEntity) {
                    sequenceEntity = new SequenceEntity();
                    sequenceEntity.id = id;
                    sequenceEntity.counter = 0;
                }

                long initialValue = sequenceEntity.counter;
                List<Long> list = new ArrayList<>(numElements);
                for (Long i = 0l; i < numElements; i++)
                    list.add(initialValue + i + 1);

                long finalValue = initialValue + numElements;
                sequenceEntity.counter = finalValue;
                ofy(client).save().entity(sequenceEntity).now();
                return list;
            }
        });

        return Collections.unmodifiableList(next);
    }
}
