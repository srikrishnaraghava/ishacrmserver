package crmdna.programtype;

import crmdna.practice.Practice.PracticeProp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProgramTypeProp {

    public long programTypeId;
    public String name;
    public String displayName;
    public List<PracticeProp> practiceProps = new ArrayList<>();

    public Set<Long> getPracticeIds() {

        Set<Long> practiceIds = new HashSet<>();
        for (PracticeProp practiceProp : practiceProps) {
            practiceIds.add(practiceProp.practiceId);
        }

        return practiceIds;
    }
}
