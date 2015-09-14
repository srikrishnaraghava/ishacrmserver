package crmdna.interaction;

public class SubInteractionProp implements Comparable<SubInteractionProp> {
    public long subInteractionId;
    public long timestamp;
    public String content;

    @Override
    public int compareTo(SubInteractionProp o) {
        // sort in descending order of time
        Long _ts = o.timestamp;
        return _ts.compareTo(timestamp);
    }
}
