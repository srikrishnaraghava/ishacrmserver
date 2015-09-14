package crmdna.member;

import crmdna.common.contact.ContactProp;
import crmdna.group.IHasGroupIdsAndNames;
import crmdna.list.ListProp;
import crmdna.member.Member.AccountType;
import crmdna.practice.IHasPracticeIdsAndNames;

import java.util.*;

public class MemberProp implements Comparable<MemberProp>, IHasGroupIdsAndNames,
        IHasPracticeIdsAndNames {
    public long memberId;
    public ContactProp contact;
    public String name;

    public Set<Long> groupIds = new HashSet<Long>();
    public Set<Long> programIds = new HashSet<Long>();
    public Set<Long> programTypeIds = new HashSet<Long>();
    public Set<Long> practiceIds = new HashSet<Long>();
    public Set<Long> subscribedListIds = new HashSet<>();
    public Set<Long> unsubscribedListIds = new HashSet<>();
    public Set<Long> listIds = new HashSet<>();
    public Set<Long> subscribedGroupIds = new HashSet<>();
    public Set<Long> unsubscribedGroupIds = new HashSet<>();

    // dependents
    public Set<String> practices = new TreeSet<>();
    public List<MemberProgramProp> memberProgramProps = new ArrayList<>();
    public Set<String> groups = new HashSet<>();
    public List<ListProp> listProps = new ArrayList<>();

    public boolean hasAccount;
    public AccountType accountType;
    public boolean isEmailVerified;
    public boolean accountDisabled;
    public long accountCreatedMS;

    public int verificationCode;

    byte[] encryptedPwd;
    byte[] salt;

    @Override
    public int compareTo(MemberProp o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public Set<Long> getGroupIds() {
        return groupIds;
    }

    @Override
    public void setGroupNames(Set<String> groupNames) {
        groups = groupNames;
    }

    @Override
    public Set<Long> getPracticeIds() {
        return practiceIds;
    }

    @Override
    public void setPracticeNames(Set<String> practiceNames) {
        practices = practiceNames;

    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getEncryptedPwd() {
        return encryptedPwd;
    }
}
