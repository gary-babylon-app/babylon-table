package app.babylon.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

class SentenceParseModeTest
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
        assertEquals(Sentence.ParseMode.EXACT, Sentence.ParseMode.parse(null));
        assertEquals(Sentence.ParseMode.FIRST_IN, Sentence.ParseMode.parse("first_in"));
        assertEquals(Sentence.ParseMode.FIRST_IN, Sentence.ParseMode.parse("firstIn"));
        assertEquals(Sentence.ParseMode.LAST_IN, Sentence.ParseMode.parse("lastIn"));
        assertEquals(Sentence.ParseMode.ONLY_IN, Sentence.ParseMode.parse("only_in"));
        assertEquals(Sentence.ParseMode.ONLY_IN, Sentence.ParseMode.parse("onlyIn"));
    }

    @Test
    void applyShouldUseRequestedSentenceMode()
    {
        assertEquals(Integer.valueOf(12), Sentence.ParseMode.EXACT.apply(INTEGER_PARSER, "12"));
        assertEquals(Integer.valueOf(12), Sentence.ParseMode.FIRST_IN.apply(INTEGER_PARSER, "x 12 y 34"));
        assertEquals(Integer.valueOf(34), Sentence.ParseMode.LAST_IN.apply(INTEGER_PARSER, "x 12 y 34"));
        assertEquals(Integer.valueOf(12), Sentence.ParseMode.ONLY_IN.apply(INTEGER_PARSER, "only 12 here"));
        assertNull(Sentence.ParseMode.ONLY_IN.apply(INTEGER_PARSER, "12 and 34"));
    }
}
