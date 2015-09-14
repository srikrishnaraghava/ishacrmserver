package crmdna.attendance;

import crmdna.client.Client;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;

import java.util.*;

public class CheckInMemberProp {
    public long memberId;
    public String name;
    public String email;
    public String phoneNos;
    public boolean allow;
    public String notAllowingReason;
    public Set<Long> practiceIds;

    // dependents
    public TreeSet<String> practices = new TreeSet<>();

    public static void populatePractices(String client, List<CheckInMemberProp> checkInMemberProps) {
        Client.ensureValid(client);

        Set<Long> practiceIds = new HashSet<>();

        for (CheckInMemberProp prop : checkInMemberProps) {
            practiceIds.addAll(prop.practiceIds);
        }

        Map<Long, PracticeProp> practiceMap = Practice.get(client, practiceIds);

        for (CheckInMemberProp prop : checkInMemberProps) {
            prop.practices = new TreeSet<>();
            for (Long practiceId : prop.practiceIds) {
                if (practiceMap.containsKey(practiceId))
                    prop.practices.add(practiceMap.get(practiceId).displayName);
            }
        }
    }
}