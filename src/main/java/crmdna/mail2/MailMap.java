package crmdna.mail2;

import crmdna.common.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;

public class MailMap {
    public enum MergeVarID {
        MAIL_ID,
        FIRST_NAME,
        LAST_NAME,
        EMAIL,
        MOBILE_PHONE,
        HOME_PHONE,
        OFFICE_PHONE,
        VERIFICATION_CODE,
        PASSWORD,
        SUBSCRIPTION_TYPE,
        AMOUNT,
        VALIDITY,
        PROGRAM_NAME,
        REGISTRATION_ID,
        DATES,
        VENUE,
        SESSIONS,
        TRANSACTION_ID,
        INVOICE_NUMBER,
    }

    private Map<String, Map<MergeVarID, String>> map = new HashMap<>();
    private Map<MergeVarID, String> globalMap = new HashMap<>();
    private Map<MergeVarID, Object> globalObjectMap = new HashMap<>();

    public void add(String email) {
        ensure(Utils.isValidEmailAddress(email), "Email is invalid");
        map.put(email, new HashMap<MergeVarID, String>());
    }

    public void add(String email, String firstName, String lastName) {
        add(email);
        add(email, MergeVarID.FIRST_NAME, firstName);
        add(email, MergeVarID.LAST_NAME, lastName);
    }

    public void add(String email, MergeVarID id, String value) {
        Map<MergeVarID, String> _map = map.get(email);
        if (_map == null) {
            _map = new HashMap<>();
        }
        _map.put(id, value);
        map.put(email, _map);
    }

    public void add(MergeVarID id, String value) {
        globalMap.put(id, value);
    }

    public void add(MergeVarID id, Object value) {
        globalObjectMap.put(id, value);
    }

    public Set<String> getEmails() {
        return map.keySet();
    }

    public long getMailId(String email) {
        return Long.parseLong(map.get(email).get(MergeVarID.MAIL_ID));
    }

    public String get(MergeVarID id, String email) {

        ensure(map.containsKey(email), "Email [" + email + "] missing in MailMap");

        return this.map.get(email).get(id);
    }

    public String get(MergeVarID id) {
        return globalMap.get(id);
    }

    public Object getObject(MergeVarID id) {
        return globalObjectMap.get(id);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void delete(String email) {
        ensureNotNull(email, "email is null");
        map.remove(email);
    }

    public void validateMergeVars() {
        Set<MergeVarID> prevSet = null;

        for (Map.Entry<String, Map<MergeVarID, String>> entry : map.entrySet()) {
            Map<MergeVarID, String> mergeVarMap = entry.getValue();
            if (prevSet == null) {
                prevSet = mergeVarMap.keySet();
            } else {
                ensure(prevSet.equals(mergeVarMap.keySet()),
                    "Number of mergeVars differ between entries");
            }
        }
    }

    public void populateMailIds() {

        Long mailId = (new Date().getTime()) * 1000000;
        for (Map.Entry<String, Map<MergeVarID, String>> entry : map.entrySet())
        {
            Map<MergeVarID, String> mergeVarMap = entry.getValue();
            mergeVarMap.put(MergeVarID.MAIL_ID, (mailId++).toString());
        }
    }




}
