package crmdna.member;

import crmdna.common.DateUtils;
import crmdna.common.DateUtils.Month;
import crmdna.programtype.IHasProgramTypeIdAndName;

public class MemberProgramProp implements Comparable<MemberProgramProp>, IHasProgramTypeIdAndName {
    public Month month;
    public int year;
    public long programTypeId;
    public String teacher;
    public String groupOrCity;
    public boolean verified;
    public Integer unverifiedProgramId;

    // dependents
    public String programType;

    // public static void populateProgramType(String client, Iterable<MemberProgramProp> props) {
    //
    // ensureNotNull(props, "props is null");
    //
    // Set<Long> programTypeIds = new HashSet<>();
    // for (MemberProgramProp prop : props) {
    // programTypeIds.add(prop.programTypeId);
    // }
    //
    // Map<Long, ProgramTypeEntity> map = ProgramType.get(client, programTypeIds);
    //
    // for (MemberProgramProp prop : props) {
    // if (map.containsKey(prop.programTypeId))
    // prop.programType = map.get(prop.programTypeId).toProp().displayName;
    // }
    // }

    @Override
    public int compareTo(MemberProgramProp arg0) {
        // sorted in descending order of time

        Integer yyyymm = year * 100 + DateUtils.getZeroBasedMonthIndex(month) + 1;
        Integer arg0yyyymm = arg0.year * 100 + DateUtils.getZeroBasedMonthIndex(arg0.month) + 1;

        return arg0yyyymm.compareTo(yyyymm);
    }

    @Override
    public long getProgramTypeId() {
        return programTypeId;
    }

    @Override
    public void setProgramTypeName(String programTypeName) {
        programType = programTypeName;
    }
}
