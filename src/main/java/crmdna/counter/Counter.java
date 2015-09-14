package crmdna.counter;

import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

public class Counter {
    public static void increment(String client, CounterType counterType, String key, int n) {
        Client.ensureValid(client);

        CounterCore counterCore = getCounterCore(counterType);
        counterCore.increment(client, getCounterName(counterType, key), n);
    }

    public static long incrementAndGetCurrentCount(String client, CounterType counterType,
                                                   String key, int n) {
        increment(client, counterType, key, n);
        ObjectifyFilter.complete();

        return getCount(client, counterType, key);

    }

    public static long incrementAndGetCurrentCount(String client, CounterType counterType, String key) {
        return incrementAndGetCurrentCount(client, counterType, key, 1);
    }

    public static long getCount(String client, CounterType counterType, String key) {
        Client.ensureValid(client);

        CounterCore counterCore = getCounterCore(counterType);
        String counterName = getCounterName(counterType, key);
        long count = counterCore.getCount(client, counterName);

        return count;
    }

    private static String getCounterName(CounterType counterType, String key) {
        return counterType + "_" + key;
    }

    private static CounterCore getCounterCore(CounterType counterType) {
        // once counter impl is fixed for a type, shards can be increased but
        // never decreased
        if (counterType == CounterType.CHECKIN)
            return new CounterCore(2); // counter with 2 shards

        throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "No implementation defined for counter type [" + counterType + "]");
    }

    public enum CounterType {
        CHECKIN
    }
}
