package crmdna.registration;

import crmdna.registration.Registration.RegistrationStatus;

import java.util.Map;
import java.util.TreeMap;

public class RegistrationSummaryProp {
    public int numCompleted;
    public int numStartedButNotCompleted;

    public Map<Integer, Integer> yyyymmddVsNumCompleted = new TreeMap<>();
    public Map<Integer, Integer> yyyymmddVsNumStartedButNotCompleted = new TreeMap<>();

    public Map<RegistrationStatus, Integer> regStatusVsNum = new TreeMap<>();
}
