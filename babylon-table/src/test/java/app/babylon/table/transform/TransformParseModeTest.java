package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

class TransformParseModeTest
{
    private static final Function<CharSequence, Integer> INTEGER_PARSER = s -> {
        if (s == null)
        {
            return null;
        }
        String text = s.toString();
        return text.matches("\\d+") ? Integer.valueOf(text) : null;
    };

    @Test
    void parseShouldDefaultToExactForNull()
    {
        assertEquals(TransformParseMode.EXACT, TransformParseMode.parse(null));
        assertEquals(TransformParseMode.FIRST_IN, TransformParseMode.parse("first_in"));
    }

    @Test
    void applyShouldUseRequestedSentenceMode()
    {
        assertEquals(Integer.valueOf(12), TransformParseMode.EXACT.apply(INTEGER_PARSER, "12"));
        assertEquals(Integer.valueOf(12), TransformParseMode.FIRST_IN.apply(INTEGER_PARSER, "x 12 y 34"));
        assertEquals(Integer.valueOf(34), TransformParseMode.LAST_IN.apply(INTEGER_PARSER, "x 12 y 34"));
        assertEquals(Integer.valueOf(12), TransformParseMode.ONLY_ONE_IN.apply(INTEGER_PARSER, "only 12 here"));
        assertNull(TransformParseMode.ONLY_ONE_IN.apply(INTEGER_PARSER, "12 and 34"));
    }
}
