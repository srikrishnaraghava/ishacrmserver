package crmdna.email;

import java.util.Set;
import java.util.TreeSet;

public class EmailProp {
    public Set<String> toEmailAddresses = new TreeSet<>();
    public String subject;
    public String bodyHtml;

    public String attachmentName;
    public String csvAttachmentData;
}
