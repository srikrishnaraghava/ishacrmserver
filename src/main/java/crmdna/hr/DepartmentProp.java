package crmdna.hr;


public class DepartmentProp implements Comparable<DepartmentProp> {
    public long departmentId;
    public String displayName;
    public String name;

    @Override
    public int compareTo(DepartmentProp o) {
        return displayName.compareTo(o.displayName);
    }
}
