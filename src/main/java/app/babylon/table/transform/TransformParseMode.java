package app.babylon.table.transform;

import java.util.function.Function;

import app.babylon.text.Sentence;
import app.babylon.text.Strings;

public enum TransformParseMode
{
    EXACT, FIRST_IN, LAST_IN, ONLY_ONE_IN;

    public static TransformParseMode parse(CharSequence s)
    {
        if (s == null)
        {
            return EXACT;
        }
        return valueOf(Strings.strip(s).toString().toUpperCase());
    }

    public <T> T apply(Function<CharSequence, T> parser, String value)
    {
        Function<CharSequence, T> sentenceParser = s -> parser.apply(s == null ? null : s.toString());
        return switch (this)
        {
            case EXACT -> parser.apply(value);
            case FIRST_IN -> Sentence.firstIn(sentenceParser, value);
            case LAST_IN -> Sentence.lastIn(sentenceParser, value);
            case ONLY_ONE_IN -> Sentence.onlyOneIn(sentenceParser, value);
        };
    }
}
