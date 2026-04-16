package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TransformParseModeTest
{
    @Test
    void parseShouldDefaultToExactForNull()
    {
        assertEquals(TransformParseMode.EXACT, TransformParseMode.parse(null));
        assertEquals(TransformParseMode.FIRST_IN, TransformParseMode.parse("first_in"));
    }

    @Test
    void applyShouldUseRequestedSentenceMode()
    {
        assertEquals(Integer.valueOf(12), TransformParseMode.EXACT.apply(s -> Integer.valueOf(s), "12"));
        assertEquals(Integer.valueOf(12),
                TransformParseMode.FIRST_IN.apply(s -> s.matches("\\d+") ? Integer.valueOf(s) : null, "x 12 y 34"));
        assertEquals(Integer.valueOf(34),
                TransformParseMode.LAST_IN.apply(s -> s.matches("\\d+") ? Integer.valueOf(s) : null, "x 12 y 34"));
        assertEquals(Integer.valueOf(12), TransformParseMode.ONLY_ONE_IN
                .apply(s -> s.matches("\\d+") ? Integer.valueOf(s) : null, "only 12 here"));
        assertNull(
                TransformParseMode.ONLY_ONE_IN.apply(s -> s.matches("\\d+") ? Integer.valueOf(s) : null, "12 and 34"));
    }
}
