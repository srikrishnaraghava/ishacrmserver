package crmdna.client.isha;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IshaProgramTest {
    @Test
    public void isSathsangTest() {
        assertEquals(true, IshaUtils.isSathsang("Sathsang"));
        assertEquals(true, IshaUtils.isSathsang("Satsang"));
        assertEquals(false, IshaUtils.isSathsang("shambhavi"));
    }
}
