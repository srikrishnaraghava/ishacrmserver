package crmdna.counter;

import com.googlecode.objectify.VoidWork;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static crmdna.common.OfyService.ofy;

class CounterCore {
    Random random = new Random();
    int numShards; // once set for a counter cannot be changed

    protected CounterCore(int numShards) {
        if (numShards < 0)
            Utils.throwIncorrectSpecException("numShards cannot be negative");

        final int MAX_SHARDS = 100;
        if (numShards > MAX_SHARDS)
            Utils.throwIncorrectSpecException("Only [" + MAX_SHARDS + "] allowed");

        this.numShards = numShards;
    }

    private String getShardKey(String counterName, int shardIndex) {
        return counterName + "_shard" + shardIndex;
    }

    public void increment(final String namespace, String counterName, final int n) {
        int shardIndex = random.nextInt(numShards);

        final String key = getShardKey(counterName, shardIndex);

        final int MAX_KEY_LENGTH = 100;
        if (key.length() > MAX_KEY_LENGTH)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Counter key [" + key + "] is more than [" + MAX_KEY_LENGTH + "] chars");

        ofy(namespace).transact(new VoidWork() {

            @Override
            public void vrun() {
                ShardEntity se = ofy(namespace).load().type(ShardEntity.class).id(key).now();
                if (se == null) {
                    se = new ShardEntity();
                    se.key = key;
                    se.count = n;
                } else {
                    se.count += n;
                }

                ofy(namespace).save().entity(se);
            }
        });
    }

    public long getCount(String namespace, String counterName) {
        List<String> keys = new ArrayList<>();
        for (int shardIndex = 0; shardIndex < numShards; shardIndex++) {
            keys.add(getShardKey(counterName, shardIndex));
        }

        Map<String, ShardEntity> map = ofy(namespace).load().type(ShardEntity.class).ids(keys);

        long count = 0;
        for (String key : map.keySet()) {
            ShardEntity se = map.get(key);
            if (se != null)
                count += se.count;
        }

        return count;
    }
}
