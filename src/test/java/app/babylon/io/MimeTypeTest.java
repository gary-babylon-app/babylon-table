package app.babylon.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MimeTypeTest
{
    @Test
    void parsesEnumNamesAndMimeNames()
    {
        assertEquals("text/plain", MimeType.TEXT_PLAIN.getName());
        assertEquals(MimeType.TEXT_PLAIN, MimeType.parse("TEXT_PLAIN"));
        assertEquals(MimeType.TEXT_PLAIN, MimeType.parse("text/plain"));
        assertEquals(MimeType.APPLICATION_PDF, MimeType.parse("application/pdf"));
        assertEquals(MimeType.MESSAGE_RFC822, MimeType.parse("MESSAGE_RFC822"));
        assertEquals(MimeType.MESSAGE_RFC822, MimeType.parse("message/rfc822"));
        assertEquals(MimeType.MESSAGE_RFC822, MimeType.parse(" \tmessage/rfc822\n"));
        assertNull(MimeType.parse("application/json"));
        assertNull(MimeType.parse(null));
    }

    @Test
    void parsesSlice()
    {
        CharSequence s = "xx  message/rfc822  yy";

        assertEquals(MimeType.MESSAGE_RFC822, MimeType.parse(s, 2, s.length() - 2));
    }
}
