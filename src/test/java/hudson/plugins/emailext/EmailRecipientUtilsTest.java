package hudson.plugins.emailext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jakarta.mail.internet.InternetAddress;
import org.junit.Test;

public class EmailRecipientUtilsTest {
    @Test
    public void testFixupDelimiters() throws Exception {
        String output;
        output = EmailRecipientUtils.fixupDelimiters("  Name   Surname   <n.Surname@mymail.com>   ");
        assertEquals("Name Surname <n.Surname@mymail.com>", output);
        InternetAddress[] addresses = InternetAddress.parse(output);
        assertEquals(1, addresses.length);
        assertEquals("Name Surname", addresses[0].getPersonal());
        assertEquals("n.Surname@mymail.com", addresses[0].getAddress());

        output = EmailRecipientUtils.fixupDelimiters(
                "user0, user1@email.com User Two    <user2@email.com> user3@email.com   ");
        assertEquals("user0, user1@email.com, User Two <user2@email.com>, user3@email.com", output);
        addresses = InternetAddress.parse(output);
        assertEquals(4, addresses.length);

        assertNull(addresses[0].getPersonal());
        assertEquals("user0", addresses[0].getAddress());

        assertNull(addresses[1].getPersonal());
        assertEquals("user1@email.com", addresses[1].getAddress());

        assertEquals("User Two", addresses[2].getPersonal());
        assertEquals("user2@email.com", addresses[2].getAddress());

        assertNull(addresses[3].getPersonal());
        assertEquals("user3@email.com", addresses[3].getAddress());
    }
}
