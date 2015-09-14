package crmdna.programtype;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProgramTypeHelper {
    public static void populateName(String client,
                                    Iterable<? extends IHasProgramTypeIdAndName> iterable) {

        // do a batch get using ids and populate names
        Set<Long> ids = new HashSet<>();
        for (IHasProgramTypeIdAndName element : iterable) {
            ids.add(element.getProgramTypeId());
        }

        Map<Long, ProgramTypeEntity> map = ProgramType.getEntities(client, ids);

        for (IHasProgramTypeIdAndName element : iterable) {

            long id = element.getProgramTypeId();

            if (!map.containsKey(id))
                continue;

            String name = map.get(id).displayName;
            element.setProgramTypeName(name);
        }
    }
}
