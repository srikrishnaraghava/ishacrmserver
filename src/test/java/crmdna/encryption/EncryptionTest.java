package crmdna.encryption;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.assertEquals;

public class EncryptionTest {

    @Test
    public void canLoginWithCorrectPassword() throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        // password: value.add
        byte[] salt = Encryption.generateRandomSalt();

        byte[] encryptedPassword = Encryption.getEncryptedPassword("value.add",
                salt);

        boolean result = Encryption.authenticate("value.add",
                encryptedPassword, salt);
        assertEquals(true, result);

        // try multiple times
        for (int i = 0; i < 20; i++) {
            result = Encryption.authenticate("value.add", encryptedPassword,
                    salt);
            assertEquals(true, result);
        }
    }

    @Test
    public void cannotLoginWithInCorrectPassword()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // password: value.add
        byte[] salt = Encryption.generateRandomSalt();

        byte[] encryptedPassword = Encryption.getEncryptedPassword("value.add",
                salt);

        assertEquals(false,
                Encryption.authenticate("value.add2", encryptedPassword, salt));
        assertEquals(false,
                Encryption.authenticate("Value.Add", encryptedPassword, salt));
    }
}
