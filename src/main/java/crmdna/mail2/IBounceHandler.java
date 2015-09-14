package crmdna.mail2;

public interface IBounceHandler {
    public void onSoftBounce(MandrillEventProp mandrillEventProp);

    public void onHardBounce(MandrillEventProp mandrillEventProp);

    public void onComplaint(MandrillEventProp mandrillEventProp);
}
