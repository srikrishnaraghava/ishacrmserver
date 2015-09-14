package crmdna.client.isha;

import java.util.Set;

public class IshaUtils {
    public static boolean isSathsang(String programName) {
        if (programName.toLowerCase().contains("sathsang")
                || programName.toLowerCase().contains("satsang"))
            return true;

        return false;
    }

    public static boolean isMeditator(Set<Long> practiceIds) {
        Set<Long> sathsangPracticeIds = IshaConfig.safeGet().sathsangPracticeIds;

        for (Long practiceId : sathsangPracticeIds) {
            if (practiceIds.contains(practiceId))
                return true;
        }

        return false;
    }
}
