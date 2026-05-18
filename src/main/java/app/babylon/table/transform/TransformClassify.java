package app.babylon.table.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public class TransformClassify extends TransformStringToString
{
    public final static String FUNCTION_NAME = "Classify";
    private static final int DEFAULT_ANCHOR_MAX_GAP = 1;

    public enum Mode
    {
        REGEX, ANCHOR
    }

    private final Mode mode;
    private final Pattern pattern;
    private final String anchorPhrase;
    private final List<String> anchorTokens;
    private final int anchorMaxGap;
    private final String newColumnFoundValue;
    private final String newColumnNotFoundValue;

    public TransformClassify(ColumnName existingColumnName, ColumnName newColumnName, Pattern pattern,
            String newColumnFoundValue, String newColumnNotFoundValue)
    {
        this(existingColumnName, newColumnName, Mode.REGEX, ArgumentCheck.nonNull(pattern), null, newColumnFoundValue,
                newColumnNotFoundValue, DEFAULT_ANCHOR_MAX_GAP);
    }

    private TransformClassify(ColumnName existingColumnName, ColumnName newColumnName, Mode mode, Pattern pattern,
            String anchorPhrase, String newColumnFoundValue, String newColumnNotFoundValue, int anchorMaxGap)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName == null ? existingColumnName : newColumnName);
        this.mode = ArgumentCheck.nonNull(mode);
        this.pattern = pattern;
        this.anchorPhrase = anchorPhrase;
        this.anchorTokens = mode == Mode.ANCHOR ? List.copyOf(tokens(anchorPhrase)) : List.of();
        this.anchorMaxGap = anchorMaxGap;
        this.newColumnFoundValue = newColumnFoundValue;
        this.newColumnNotFoundValue = newColumnNotFoundValue;
    }

    public static TransformClassify of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 5)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]), Pattern.compile(params[2]), params[3],
                    params[4]);
        }
        return null;
    }

    public static TransformClassify of(ColumnName existingColumnName, ColumnName newColumnName, Pattern pattern,
            String newColumnFoundValue, String newColumnNotFoundValue)
    {
        return new TransformClassify(existingColumnName, newColumnName, pattern, newColumnFoundValue,
                newColumnNotFoundValue);
    }

    public static TransformClassify anchor(ColumnName existingColumnName, ColumnName newColumnName, String anchorPhrase,
            String newColumnFoundValue, String newColumnNotFoundValue)
    {
        return new TransformClassify(existingColumnName, newColumnName, Mode.ANCHOR, null,
                ArgumentCheck.nonEmpty(anchorPhrase), newColumnFoundValue, newColumnNotFoundValue,
                DEFAULT_ANCHOR_MAX_GAP);
    }

    public String newColumnNotFoundValue()
    {
        return this.newColumnNotFoundValue;
    }

    public Pattern pattern()
    {
        return this.pattern;
    }

    public Mode mode()
    {
        return this.mode;
    }

    public String anchorPhrase()
    {
        return this.anchorPhrase;
    }

    public String newColumnFoundValue()
    {
        return this.newColumnFoundValue;
    }

    public String effectiveNewColumnNotFoundValue()
    {
        return this.newColumnNotFoundValue;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return s;
        }
        if (matches(s))
        {
            return newColumnFoundValue;
        }
        return effectiveNewColumnNotFoundValue();
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        ColumnName target = effectiveNewColumnName();
        return target.equals(this.existingColumnName)
                ? List.of(this.existingColumnName)
                : List.of(this.existingColumnName, target);
    }

    @Override
    public Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        Column column = columnsByName.get(this.existingColumnName);
        if (!Columns.isStringColumn(column))
        {
            return null;
        }
        ColumnObject<String> strings = Columns.asStringColumn(column);
        Column targetColumn = columnsByName.get(effectiveNewColumnName());
        ColumnObject<String> target = Columns.isStringColumn(targetColumn)
                ? Columns.asStringColumn(targetColumn)
                : null;
        ColumnObject.Builder<String> transformed = builder(strings, target);
        for (int i = 0; i < strings.size(); ++i)
        {
            if (target != null && target.isSet(i))
            {
                transformed.add(target.get(i));
            }
            else if (strings.isSet(i))
            {
                transformed.add(transformString(strings.get(i)));
            }
            else
            {
                transformed.addNull();
            }
        }
        return transformed.build();
    }

    private ColumnObject.Builder<String> builder(ColumnObject<String> source, ColumnObject<String> target)
    {
        ColumnName name = effectiveNewColumnName();
        if (target instanceof ColumnCategorical<?> || source instanceof ColumnCategorical<?>)
        {
            return ColumnCategorical.builder(name, ColumnTypes.STRING);
        }
        return ColumnObject.builder(name, ColumnTypes.STRING);
    }

    private boolean matches(String s)
    {
        return switch (this.mode)
        {
            case REGEX -> this.pattern.matcher(s).find();
            case ANCHOR -> anchorMatches(s);
        };
    }

    private boolean anchorMatches(String s)
    {
        List<String> sourceTokens = tokens(s);
        if (this.anchorTokens.isEmpty() || sourceTokens.isEmpty())
        {
            return false;
        }
        for (int start = 0; start < sourceTokens.size(); ++start)
        {
            if (matchesFrom(sourceTokens, start))
            {
                return true;
            }
        }
        return false;
    }

    private boolean matchesFrom(List<String> sourceTokens, int start)
    {
        if (!sourceTokens.get(start).equals(this.anchorTokens.get(0)))
        {
            return false;
        }
        int previous = start;
        for (int anchor = 1; anchor < this.anchorTokens.size(); ++anchor)
        {
            int next = findNext(sourceTokens, this.anchorTokens.get(anchor), previous + 1,
                    previous + this.anchorMaxGap + 2);
            if (next < 0)
            {
                return false;
            }
            previous = next;
        }
        return true;
    }

    private static int findNext(List<String> sourceTokens, String token, int start, int endExclusive)
    {
        int end = Math.min(sourceTokens.size(), endExclusive);
        for (int i = start; i < end; ++i)
        {
            if (sourceTokens.get(i).equals(token))
            {
                return i;
            }
        }
        return -1;
    }

    private static List<String> tokens(String s)
    {
        List<String> tokens = new ArrayList<>();
        if (s == null)
        {
            return tokens;
        }
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c))
            {
                token.append(Character.toLowerCase(c));
            }
            else if (token.length() > 0)
            {
                tokens.add(token.toString().toLowerCase(Locale.ROOT));
                token.setLength(0);
            }
        }
        if (token.length() > 0)
        {
            tokens.add(token.toString().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }
}
