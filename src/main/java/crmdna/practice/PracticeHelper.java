package crmdna.practice;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PracticeHelper {
    public static void populateName(String client,
                                    Iterable<? extends IHasPracticeIdsAndNames> iterable) {

        // do a batch get using ids and populate names

        Set<Long> ids = new HashSet<>();
        for (IHasPracticeIdsAndNames element : iterable) {
            ids.addAll(element.getPracticeIds());
        }

        Map<Long, PracticeEntity> map = Practice.getEntities(client, ids);

        for (IHasPracticeIdsAndNames element : iterable) {

            Set<String> names = new HashSet<>();
            for (Long id : element.getPracticeIds()) {
                if (!map.containsKey(id))
                    continue;

                names.add(map.get(id).displayName);
            }
            element.setPracticeNames(names);
        }
    }
}
