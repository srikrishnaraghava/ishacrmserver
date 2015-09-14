package crmdna.practice;

import java.util.Set;

public interface IHasPracticeIdsAndNames {
    public Set<Long> getPracticeIds();

    public void setPracticeNames(Set<String> practiceNames);
}
