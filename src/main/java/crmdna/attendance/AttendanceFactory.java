package crmdna.attendance;

import crmdna.client.Client;

public class AttendanceFactory {
    public static IAttendance getImpl(String client) {
        Client.ensureValid(client);

        if (client.equals("isha"))
            return new AttendanceIshaImpl(client);

        return new AttendanceDefaultImpl(client);
    }
}
