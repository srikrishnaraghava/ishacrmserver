package crmdna.member;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class MemberQueryCondition {
    public String searchStr;
    public String email;
    public Set<String> firstName3Chars = new TreeSet<>();
    public Set<Long> groupIds = new HashSet<>();
    public Set<Long> programIds = new HashSet<>();
    public Set<Long> practiceIds = new HashSet<>();
    public Set<Long> programTypeIds = new HashSet<>();
    public Boolean hasAccount;
    public Set<Long> subscribedListIds = new HashSet<>();
    public Set<Long> unsubscribedListIds = new HashSet<>();
    public Set<Long> subscribedGroupIds = new HashSet<>();
    public Set<Long> unsubscribedGroupIds = new HashSet<>();
    public Set<Long> listIds = new HashSet<>();
    public String client;
    public Integer maxResultSize;
    public String nameFirstChar;
    public Set<Long> occupations = new HashSet<>();
    public String cursor;

    public MemberQueryCondition(String client, Integer maxResultSize) {
        this.client = client;
        this.maxResultSize = maxResultSize;
    }

    public Set<String> projectionFields = new HashSet<>();

    public MemberQueryCondition() {
        //required for gson deserialization
    }
}
