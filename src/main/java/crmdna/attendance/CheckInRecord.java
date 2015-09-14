package crmdna.attendance;

import java.io.Serializable;
import java.util.Date;

//@Embed
public class CheckInRecord implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    boolean isCheckin; // true for check in, false for checkout
    Date timestamp;
    int sessionDateYYYYMMDD;
    int batchNo;
    String login;
}
