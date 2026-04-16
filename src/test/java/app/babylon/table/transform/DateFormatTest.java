package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DateFormatTest
{
    @Test
    void parseShouldHandleKnownFormatsAndRejectUnknownValues()
    {
        assertEquals(DateFormat.DMY, DateFormat.parse("dmy"));
        assertEquals(DateFormat.MDY, DateFormat.parse(" MDY "));
        assertEquals(DateFormat.YMD, DateFormat.parse("ymd"));
        assertEquals(null, DateFormat.parse("excel"));
        assertEquals(null, DateFormat.parse(" "));
        assertEquals(null, DateFormat.parse(null));
    }
}
