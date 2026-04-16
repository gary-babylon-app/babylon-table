package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InputColumnsModeTest
{
    @Test
    void parseShouldHandleKnownModes()
    {
        assertEquals(InputColumnsMode.RETAIN, InputColumnsMode.parse("retain"));
        assertEquals(InputColumnsMode.REMOVE, InputColumnsMode.parse(" REMOVE "));
        assertEquals(null, InputColumnsMode.parse(" "));
        assertEquals(null, InputColumnsMode.parse(null));
    }

    @Test
    void parseShouldRejectUnknownModes()
    {
        assertThrows(IllegalArgumentException.class, () -> InputColumnsMode.parse("other"));
    }
}
