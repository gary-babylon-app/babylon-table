package app.babylon.table.transform;

import java.util.function.Function;

import app.babylon.text.Sentence;
import app.babylon.text.SliceParser;
import app.babylon.text.Strings;

public enum TransformParseMode
{
    EXACT, FIRST_IN, LAST_IN, ONLY_IN;

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
        return apply(SliceParser.from(parser), value);
    }

    public <T> T apply(SliceParser<T> parser, String value)
    {
        return switch (this)
        {
            case EXACT -> parser.parse(value);
            case FIRST_IN -> Sentence.firstIn(parser, value);
            case LAST_IN -> Sentence.lastIn(parser, value);
            case ONLY_IN -> Sentence.onlyIn(parser, value);
        };
    }
}
